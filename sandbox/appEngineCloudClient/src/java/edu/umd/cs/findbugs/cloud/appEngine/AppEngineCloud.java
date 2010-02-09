package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.protobuf.GeneratedMessage;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppEngineCloud extends AbstractCloud {

	private static final int BUG_UPLOAD_PARTITION_SIZE = 20;
	private static final int HASH_CHECK_PARTITION_SIZE = 50;
	private static final int EVALUATION_CHECK_SECS = 5*60;

	private static final Logger LOGGER = Logger.getLogger(AppEngineCloud.class.getName());

	/** For debugging */
	private static final boolean FORCE_UPLOAD_ALL_ISSUES = false;

	private Map<String, Issue> issuesByHash = new ConcurrentHashMap<String, Issue>();

	private String host;
	private long sessionId;
	private String user;

	private Executor backgroundExecutor;
	private @CheckForNull ExecutorService backgroundExecutorService;
	private Timer timer;

	private long mostRecentEvaluationMillis = 0;

	public AppEngineCloud(CloudPlugin plugin, BugCollection bugs) {
		this(plugin, bugs, null);
	}

	/** for testing */
	AppEngineCloud(CloudPlugin plugin, BugCollection bugs, @CheckForNull Executor executor) {
		super(plugin, bugs);
		if (executor == null) {
			backgroundExecutorService = Executors.newCachedThreadPool();
			backgroundExecutor = backgroundExecutorService;
		} else {
			backgroundExecutorService = null;
			backgroundExecutor = executor;
		}
	}

	// ====================== initialization =====================

	public boolean availableForInitialization() {
		return true;
	}

	public boolean initialize() {
		setStatusMsg("Signing into FindBugs Cloud");
		AppEngineNameLookup lookerupper = new AppEngineNameLookup();
		if (!lookerupper.initialize(plugin, bugCollection)) {
			return false;
		}
		sessionId = lookerupper.getSessionId();
		user = lookerupper.getUsername();
		host = lookerupper.getHost();
        if (user == null || host == null) {
            System.err.println("No App Engine Cloud username or hostname found! Check etc/findbugs.xml");
            return false;
        }

		if (timer != null)
			timer.cancel();
		timer = new Timer("App Engine Cloud evaluation updater", true);
		int periodMillis = EVALUATION_CHECK_SECS*1000;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				updateEvaluationsFromServer();
			}
		}, periodMillis, periodMillis);
		return true;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		timer.cancel();
		if (backgroundExecutorService != null) {
			backgroundExecutorService.shutdownNow();
		}
	}

	// =============== accessors ===================

	public String getUser() {
		return user;
	}

	public BugDesignation getPrimaryDesignation(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		return e == null ? null : createBugDesignation(e);
	}

	public long getFirstSeen(BugInstance b) {
        long firstSeenFromCloud = getFirstSeenFromCloud(b);
        long firstSeenLocally = super.getFirstSeen(b);
        return Math.min(firstSeenFromCloud, firstSeenLocally);
	}

    @Override
	protected Iterable<BugDesignation> getAllUserDesignations(BugInstance bd) {
		List<BugDesignation> list = new ArrayList<BugDesignation>();
		for (Evaluation eval : issuesByHash.get(bd.getInstanceHash()).getEvaluationsList()) {
			list.add(createBugDesignation(eval));
		}
		return list;
	}

	@Override
	public URL getBugLink(BugInstance b) {
		try {
			return new URL(new GoogleCodeBugFiler(this, "findbugs").file(b));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public BugFilingStatus getBugLinkStatus(BugInstance b) {
		return BugFilingStatus.FILE_BUG;
	}

	@Override
	public boolean supportsBugLinks() {
		return true;
	}

	// ================== mutators ================

	public void bugsPopulated() {
		Map<String, BugInstance> bugsByHash = new HashMap<String, BugInstance>();

		for(BugInstance b : bugCollection.getCollection()) {
			bugsByHash.put(b.getInstanceHash(), b);
		}
		List<String> allHashes = new ArrayList<String>(bugsByHash.keySet());

		assert bugsByHash.size() == allHashes.size();

		int numBugs = bugsByHash.size();
		setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud...");

		// send all instance hashes to server
		try {

            HttpURLConnection conn = openConnection("/log-in");
            conn.setDoOutput(true);
            conn.connect();

            OutputStream stream = conn.getOutputStream();
            LogIn logIn = LogIn.newBuilder()
                    .setSessionId(sessionId)
                    .setAnalysisTimestamp(bugCollection.getAnalysisTimestamp())
                    .build();
            logIn.writeTo(stream);
            stream.close();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IllegalStateException("Could not log into cloud - error " 
                                                + responseCode + conn.getResponseMessage());
            }

            for (int i = 0; i < numBugs; i += HASH_CHECK_PARTITION_SIZE) {
                setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud..."
						+ (i * 100 / numBugs) + "%");
				List<String> hashesToCheck = allHashes.subList(i, Math.min(i+HASH_CHECK_PARTITION_SIZE, numBugs));
				FindIssuesResponse response = submitHashes(hashesToCheck);
				System.out.println("Checking " + hashesToCheck);
				System.out.println("Found " + response.getFoundIssuesCount());
				for (Issue issue : response.getFoundIssuesList()) {
					storeProtoIssue(issue);

					BugInstance bugInstance;
					String hash = AppEngineProtoUtil.decodeHash(issue.getHash());
					if (FORCE_UPLOAD_ALL_ISSUES) // don't remove anything from bugsByHash
						bugInstance = bugsByHash.get(hash);
					else
						bugInstance = bugsByHash.remove(hash);

					if (bugInstance != null) {
						updateBugInstanceAndNotify(bugInstance);
					} else {
						LOGGER.warning("Server sent back issue that we don't know about: " + hash + " - " + issue);
					}
				}
			}
			if (!bugsByHash.values().isEmpty()) {
				final List<BugInstance> newBugs = new ArrayList<BugInstance>(bugsByHash.values());
				System.out.println("Server didn't know " + bugsByHash);
				backgroundExecutor.execute(new Runnable() {
					public void run() {
						try {
							uploadNewBugs(newBugs);
						} catch (IOException e) {
							LOGGER.log(Level.WARNING, "Error while uploading new bugs", e);
						}
					}
				});
			} else {
				setStatusMsg("All " + numBugs + " bugs are already stored in the FindBugs Cloud");
			}

		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void uploadNewBugs(List<BugInstance> newBugs) throws IOException {
		try {
			for (int i = 0; i < newBugs.size(); i += BUG_UPLOAD_PARTITION_SIZE) {
				setStatusMsg("Uploading " + newBugs.size()
						+ " new bugs to the FindBugs Cloud..." + i * 100
						/ newBugs.size() + "%");
				uploadIssues(newBugs.subList(i, Math.min(newBugs.size(), i + BUG_UPLOAD_PARTITION_SIZE)));
			}
		} finally {
			setStatusMsg("");
		}
	}

	public void bugFiled(BugInstance b, Object bugLink) {
		System.out.println("bug filed: " + b + ": " + bugLink);
	}

	@SuppressWarnings("deprecation")
	public void storeUserAnnotation(BugInstance bugInstance) {
		BugDesignation designation = bugInstance.getNonnullUserDesignation();
		Builder evalBuilder = Evaluation.newBuilder()
				.setWhen(designation.getTimestamp())
				.setDesignation(designation.getDesignationKey());
		String comment = designation.getAnnotationText();
		if (comment != null) {
			evalBuilder.setComment(comment);
		}
		UploadEvaluation uploadMsg = UploadEvaluation.newBuilder()
				.setSessionId(sessionId)
				.setHash(AppEngineProtoUtil.encodeHash(bugInstance.getInstanceHash()))
				.setEvaluation(evalBuilder.build())
				.build();

		openPostUrl(uploadMsg, "/upload-evaluation", 1);
	}

	/** package-private for testing */
	void updateEvaluationsFromServer() {
		setStatusMsg("Checking FindBugs Cloud for updates");

		RecentEvaluations evals;
		try {
			evals = getRecentEvaluationsFromServer();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		if (evals.getIssuesCount() > 0)
			setStatusMsg("Checking FindBugs Cloud for updates...found " + evals.getIssuesCount());
		else
			setStatusMsg("");
		for (Issue issue : evals.getIssuesList()) {
			Issue existingIssue = issuesByHash.get(AppEngineProtoUtil.decodeHash(issue.getHash()));
			if (existingIssue != null) {
				Issue newIssue = mergeIssues(existingIssue, issue);
				assert newIssue.getHash().equals(issue.getHash());
				storeProtoIssue(newIssue);
				BugInstance bugInstance = getBugByHash(AppEngineProtoUtil.decodeHash(issue.getHash()));
				if (bugInstance != null) {
					updateBugInstanceAndNotify(bugInstance);
				}
			}
		}
	}

	// ==================== for testing ===========================

	/** package-private for testing */
	void setSessionId(long id) {
		this.sessionId = id;
	}

	/** package-private for testing */
	void setUsername(String user) {
		this.user = user;
	}

	// ================== private methods ======================

	private long getFirstSeenFromCloud(BugInstance b) {
        Issue issue = issuesByHash.get(b.getInstanceHash());
        if (issue == null)
            return Long.MAX_VALUE;
        return issue.getFirstSeen();
    }

	private BugDesignation createBugDesignation(Evaluation e) {
		return new BugDesignation(e.getDesignation(), e.getWhen(),
								  e.getComment(), e.getWho());
	}

	private void storeProtoIssue(Issue newIssue) {
		for (Evaluation eval : newIssue.getEvaluationsList()) {
			if (eval.getWhen() > mostRecentEvaluationMillis) {
				mostRecentEvaluationMillis = eval.getWhen();
			}
		}
		issuesByHash.put(AppEngineProtoUtil.decodeHash(newIssue.getHash()), newIssue);
	}

	private FindIssuesResponse submitHashes(List<String> bugsByHash)
			throws IOException {
		LOGGER.info("Checking " + bugsByHash.size() + " bugs against App Engine Cloud");
		FindIssues hashList = FindIssues.newBuilder()
				.setSessionId(sessionId)
				.addAllMyIssueHashes(AppEngineProtoUtil.encodeHashes(bugsByHash))
				.build();

		long start = System.currentTimeMillis();
		HttpURLConnection conn = openConnection("/find-issues");
		conn.setDoOutput(true);
		conn.connect();
		LOGGER.info("Connected in " + (System.currentTimeMillis() - start) + "ms");

		start = System.currentTimeMillis();
		OutputStream stream = conn.getOutputStream();
		hashList.writeTo(stream);
		stream.close();
		long elapsed = System.currentTimeMillis() - start;
		LOGGER.info("Submitted hashes (" + hashList.getSerializedSize()/1024 + " KB) in " + elapsed + "ms ("
				+ (elapsed/bugsByHash.size()) + "ms per hash)");

		start = System.currentTimeMillis();
		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			LOGGER.info("Error " + responseCode + ", took " + (System.currentTimeMillis() - start) + "ms");
			throw new IOException("Response code " + responseCode + " : " + conn.getResponseMessage());
		}
		FindIssuesResponse response = FindIssuesResponse.parseFrom(conn.getInputStream());
		conn.disconnect();
		int foundIssues = response.getFoundIssuesCount();
		elapsed = System.currentTimeMillis()-start;
		LOGGER.info("Received " + foundIssues + " bugs from server in " + elapsed + "ms ("
				+ (elapsed/(foundIssues+1)) + "ms per bug)");
		return response;
	}

	/** package-private for testing */
	void uploadIssues(Collection<BugInstance> bugsToSend)
			throws IOException {
		LOGGER.info("Uploading " + bugsToSend.size() + " bugs to App Engine Cloud");
		UploadIssues.Builder issueList = UploadIssues.newBuilder();
		issueList.setSessionId(sessionId);
		for (BugInstance bug: bugsToSend) {
			issueList.addNewIssues(ProtoClasses.Issue.newBuilder()
					.setHash(AppEngineProtoUtil.encodeHash(bug.getInstanceHash()))
					.setBugPattern(bug.getType())
					.setPriority(bug.getPriority())
					.setPrimaryClass(bug.getPrimaryClass().getClassName())
					.setFirstSeen(getFirstSeen(bug))
					.build());
		}
		openPostUrl(issueList.build(), "/upload-issues", bugsToSend.size());

	}

	/** package-private for testing */
	HttpURLConnection openConnection(String url) throws IOException {
		URL u = new URL(host + url);
		return (HttpURLConnection) u.openConnection();
	}

	@SuppressWarnings("deprecation")
	private void updateBugInstanceAndNotify(BugInstance bugInstance) {
		BugDesignation primaryDesignation = getPrimaryDesignation(bugInstance);
		if (primaryDesignation != null) {
			bugInstance.setUserDesignation(primaryDesignation);
			updatedIssue(bugInstance);
		}
	}

	private Issue mergeIssues(Issue existingIssue, Issue updatedIssue) {
		List<Evaluation> allEvaluations = new ArrayList<Evaluation>();
		allEvaluations.addAll(existingIssue.getEvaluationsList());
		allEvaluations.addAll(updatedIssue.getEvaluationsList());
		removeAllButLatestEvaluationPerUser(allEvaluations);

		return Issue.newBuilder(existingIssue)
				.clearEvaluations()
				.addAllEvaluations(allEvaluations)
				.build();
	}

	private void removeAllButLatestEvaluationPerUser(List<Evaluation> allEvaluations) {
		Set<String> seenUsernames = new HashSet<String>();
		for (ListIterator<Evaluation> it = allEvaluations.listIterator(allEvaluations.size()); it.hasPrevious();) {
			Evaluation evaluation = it.previous();
			boolean isNewUsername = seenUsernames.add(evaluation.getWho());
			if (!isNewUsername)
				it.remove();
		}
	}

	private RecentEvaluations getRecentEvaluationsFromServer() throws IOException {
		HttpURLConnection conn = openConnection("/get-recent-evaluations");
		conn.setDoOutput(true);
		try {
			OutputStream outputStream = conn.getOutputStream();
			GetRecentEvaluations.newBuilder()
					.setSessionId(sessionId)
					.setTimestamp(mostRecentEvaluationMillis)
					.build()
					.writeTo(outputStream);
			outputStream.close();
			if (conn.getResponseCode() != 200) {
				throw new IllegalStateException(
						"server returned error code "
								+ conn.getResponseCode() + " "
								+ conn.getResponseMessage());
			}
			return RecentEvaluations.parseFrom(conn.getInputStream());
		} finally {
			conn.disconnect();
		}
	}

	private Evaluation getMostRecentEvaluation(BugInstance b) {
		Issue issue = issuesByHash.get(b.getInstanceHash());
		if (issue == null)
			return null;
		Evaluation mostRecent = null;
		long when = Long.MIN_VALUE;
		for(Evaluation e : issue.getEvaluationsList())
			if (e.getWho().equals(getUser()) && e.getWhen() > when) {
				mostRecent = e;
				when = e.getWhen();
			}

		return mostRecent;
	}

	private void openPostUrl(GeneratedMessage uploadMsg, String url, int items) {
		try {
			long start = System.currentTimeMillis();
			HttpURLConnection conn = openConnection(url);
			conn.setDoOutput(true);
			conn.connect();
			LOGGER.info("Connected in " + (System.currentTimeMillis() - start) + "ms");
			try {
				if (uploadMsg != null) {
					start = System.currentTimeMillis();
					OutputStream stream = conn.getOutputStream();
					uploadMsg.writeTo(stream);
					stream.close();
					long elapsed = System.currentTimeMillis() - start;
					conn.getResponseCode(); // wait for response
					LOGGER.info("Uploaded " + uploadMsg.getSerializedSize()/1024 + " KB in "
							+ elapsed + "ms (" + (elapsed/(items+1)) + " per item)");
				}
				int responseCode = conn.getResponseCode();
				if (responseCode != 200) {
					throw new IllegalStateException(
							"server returned error code "
									+ conn.getResponseCode() + " "
									+ conn.getResponseMessage());
				}
			} finally {
				conn.disconnect();
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public Collection<String> getProjects(String className) {
		return Collections.emptyList();
	}

}
