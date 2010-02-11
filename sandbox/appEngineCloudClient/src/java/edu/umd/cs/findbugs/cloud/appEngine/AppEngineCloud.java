package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.data.projecthosting.IssuesEntry;
import com.google.gdata.util.ServiceException;
import com.google.protobuf.GeneratedMessage;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
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
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHash;
import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHash;

public class AppEngineCloud extends AbstractCloud {

	private static final int BUG_UPLOAD_PARTITION_SIZE = 10;
	private static final int HASH_CHECK_PARTITION_SIZE = 20;
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
        if (timer != null)
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
        if (getBugLinkStatus(b) == BugFilingStatus.FILE_BUG) {
		    try {
                return fileBug(b);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            String url = issuesByHash.get(b.getInstanceHash()).getBugLink();
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                LOGGER.log(Level.SEVERE, "Invalid bug link URL " + url, e);
                return null;
            }
        }
	}

    private URL fileBug(BugInstance b) throws IOException, ServiceException, OAuthException, InterruptedException {
        IssuesEntry googleCodeIssue = fileBugOnGoogleCode(b);
        if (googleCodeIssue == null)
            return null;

        String viewUrl = googleCodeIssue.getHtmlLink().getHref();
        if (viewUrl == null) {
            LOGGER.warning("Filed issue on Google Code, but URL is missing!");
            return null;
        }

        setBugLinkOnCloud(b, viewUrl);

        // update local DB
        String hash = b.getInstanceHash();
        storeIssueDetails(hash, Issue.newBuilder(issuesByHash.get(hash)).setBugLink(viewUrl).build());

        return new URL(viewUrl);
    }

    private void setBugLinkOnCloud(BugInstance b, String bugLink) throws IOException {
        HttpURLConnection conn = openConnection("/set-bug-link");
        conn.setDoOutput(true);
        try {
            OutputStream outputStream = conn.getOutputStream();
            SetBugLink.newBuilder()
                    .setSessionId(sessionId)
                    .setHash(encodeHash(b.getInstanceHash()))
                    .setUrl(bugLink)
                    .build()
                    .writeTo(outputStream);
            outputStream.close();
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException(
                        "server returned error code "
                                + conn.getResponseCode() + " "
                                + conn.getResponseMessage());
            }
        } finally {
            conn.disconnect();
        }
    }

    private IssuesEntry fileBugOnGoogleCode(BugInstance b)
            throws IOException, ServiceException, OAuthException, InterruptedException {
        IGuiCallback guiCallback = bugCollection.getProject().getGuiCallback();
        String projectName = guiCallback.showQuestionDialog(
                "Issue will be filed at Google Code.\n" +
                "\n" +
                "Google Code project name:", "Google Code Issue Tracker", "");
        if (projectName == null || projectName.trim().length() == 0) {
            return null;
        }
        GoogleCodeBugFiler bugFiler = new GoogleCodeBugFiler(this, projectName);
        IssuesEntry issue = bugFiler.file(b);
        if (issue == null)
            return null;
        return issue;
    }

    @Override
	public BugFilingStatus getBugLinkStatus(BugInstance b) {
        Issue issue = issuesByHash.get(b.getInstanceHash());
        if (issue == null)
            return BugFilingStatus.NA;
        return issue.hasBugLink() ? BugFilingStatus.VIEW_BUG : BugFilingStatus.FILE_BUG;
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

        int numBugs = bugsByHash.size();
		setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud...");

		try {
            logIntoCloud();

            checkHashes(new ArrayList<String>(bugsByHash.keySet()), bugsByHash);

            Collection<BugInstance> newBugs = bugsByHash.values();
            if (!newBugs.isEmpty()) {
                System.out.println("Server didn't know " + bugsByHash);
                uploadBugsInBackground(new ArrayList<BugInstance>(newBugs));
            } else {
				setStatusMsg("All " + numBugs + " bugs are already stored in the FindBugs Cloud");
			}

		} catch (Exception e) {
			throw new IllegalStateException(e);
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
				.setHash(encodeHash(bugInstance.getInstanceHash()))
				.setEvaluation(evalBuilder.build())
				.build();

		openPostUrl(uploadMsg, "/upload-evaluation");
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
			Issue existingIssue = issuesByHash.get(decodeHash(issue.getHash()));
			if (existingIssue != null) {
				Issue newIssue = mergeIssues(existingIssue, issue);
				assert newIssue.getHash().equals(issue.getHash());
				storeIssueDetails(decodeHash(issue.getHash()), newIssue);
				BugInstance bugInstance = getBugByHash(decodeHash(issue.getHash()));
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

    private void uploadBugsInBackground(final List<BugInstance> newBugs) {
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                try {
                    uploadNewBugs(newBugs);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error while uploading new bugs", e);
                }
            }
        });
    }

    private void logIntoCloud() throws IOException {
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
    }

    private void checkHashes(List<String> hashes, Map<String, BugInstance> bugsByHash) throws IOException {
        int numBugs = hashes.size();
        for (int i = 0; i < numBugs; i += HASH_CHECK_PARTITION_SIZE) {
            setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud..."
                    + (i * 100 / numBugs) + "%");
            List<String> partition = hashes.subList(i, Math.min(i+HASH_CHECK_PARTITION_SIZE, numBugs));
            checkHashesPartition(partition, bugsByHash);
        }
    }

    private void checkHashesPartition(List<String> hashes, Map<String, BugInstance> bugsByHash) throws IOException {
        FindIssuesResponse response = submitHashes(hashes);

        for (int j = 0; j < hashes.size(); j++) {
            String hash = hashes.get(j);
            Issue issue = response.getFoundIssues(j);

            if (isEmpty(issue))
                // the issue was not found!
                continue;

            storeIssueDetails(hash, issue);

            BugInstance bugInstance;
            if (FORCE_UPLOAD_ALL_ISSUES) bugInstance = bugsByHash.get(hash);
            else bugInstance = bugsByHash.remove(hash);

            if (bugInstance == null) {
                LOGGER.warning("Server sent back issue that we don't know about: " + hash + " - " + issue);
                continue;
            }

            updateBugInstanceAndNotify(bugInstance);
        }
    }

    private boolean isEmpty(Issue issue) {
        return !issue.hasFirstSeen() && !issue.hasLastSeen() && issue.getEvaluationsCount() == 0;
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

	private void storeIssueDetails(String hash, Issue newIssue) {
		for (Evaluation eval : newIssue.getEvaluationsList()) {
			if (eval.getWhen() > mostRecentEvaluationMillis) {
				mostRecentEvaluationMillis = eval.getWhen();
			}
		}
		issuesByHash.put(hash, newIssue);
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
					.setHash(encodeHash(bug.getInstanceHash()))
					.setBugPattern(bug.getType())
					.setPriority(bug.getPriority())
					.setPrimaryClass(bug.getPrimaryClass().getClassName())
					.setFirstSeen(getFirstSeen(bug))
					.build());
		}
		openPostUrl(issueList.build(), "/upload-issues");

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
		for (ListIterator<Evaluation> it = reverseIterator(allEvaluations); it.hasPrevious();) {
			Evaluation evaluation = it.previous();
			boolean isNewUsername = seenUsernames.add(evaluation.getWho());
			if (!isNewUsername)
				it.remove();
		}
	}

    private <E> ListIterator<E> reverseIterator(List<E> list) {
        return list.listIterator(list.size());
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

	private void openPostUrl(GeneratedMessage uploadMsg, String url) {
		try {
			HttpURLConnection conn = openConnection(url);
			conn.setDoOutput(true);
			conn.connect();
			try {
				if (uploadMsg != null) {
					OutputStream stream = conn.getOutputStream();
					uploadMsg.writeTo(stream);
					stream.close();
					conn.getResponseCode(); // wait for response
				}
				int responseCode = conn.getResponseCode();
				if (responseCode != 200) {
					throw new IllegalStateException(
							"server returned error code when opening " + url + ": "
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
