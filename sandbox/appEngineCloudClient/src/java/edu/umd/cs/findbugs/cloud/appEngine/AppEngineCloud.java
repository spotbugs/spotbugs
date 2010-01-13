package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.GeneratedMessage;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation.Builder;
import edu.umd.cs.findbugs.cloud.db.AppEngineNameLookup;

public class AppEngineCloud extends AbstractCloud {

	private Map<String, Issue> issuesByHash = new HashMap<String, Issue>();

	private long sessionId;
	private String user;

	public AppEngineCloud(BugCollection bugs) {
		super(bugs);
	}

	// ====================== initialization =====================

	public boolean availableForInitialization() {
		return true;
	}

	public boolean initialize() {
		AppEngineNameLookup lookerupper = new AppEngineNameLookup();
		if (!lookerupper.init()) {
			return false;
		}
		sessionId = lookerupper.getSessionId();
		user = lookerupper.getUsername();

		bugsPopulated();
		return true;
	}

	// =============== accessors ===================

	public String getUser() {
		return user;
	}

	public BugDesignation getPrimaryDesignation(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		return new BugDesignation(e.getDesignation(), e.getWhen(),
								  e.getComment(), e.getWho());
	}

	public long getFirstSeen(BugInstance b) {
		Issue issue = issuesByHash.get(b.getInstanceHash());
		if (issue == null)
			return Long.MAX_VALUE;
		return issue.getFirstSeen();

	}

	// ================== mutators ================

	@SuppressWarnings("deprecation")
	public void bugsPopulated() {
		Map<String, BugInstance> bugsByHash = new HashMap<String, BugInstance>();

		for(BugInstance b : bugCollection.getCollection()) {
			bugsByHash.put(b.getInstanceHash(), b);
		}

		// send all instance hashes to server
		try {
			LogInResponse response = submitHashes(bugsByHash);
			for (Issue issue : response.getFoundIssuesList()) {
				issuesByHash.put(issue.getHash(), issue);
				BugInstance bugInstance = bugsByHash.remove(issue.getHash());
				if (bugInstance != null) {
					BugDesignation primaryDesignation = getPrimaryDesignation(bugInstance);
					bugInstance.setUserDesignation(primaryDesignation);
					updatedIssue(bugInstance);
				}
			}
			Collection<BugInstance> newBugs = bugsByHash.values();
			if (!newBugs.isEmpty()) {
				uploadIssues(newBugs);
			}

		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void bugFiled(BugInstance b, Object bugLink) {
		throw new UnsupportedOperationException();
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
				.setHash(bugInstance.getInstanceHash())
				.setEvaluation(evalBuilder.build())
				.build();

		openUrl(uploadMsg, "/upload-evaluation");
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

	private @CheckForNull LogInResponse submitHashes(Map<String, BugInstance> bugsByHash)
			throws IOException, MalformedURLException {
		HttpURLConnection conn = openConnection("/find-issues");
		conn.setDoOutput(true);
		conn.connect();
		LogIn hashList = LogIn.newBuilder()
				.setAnalysisTimestamp(bugCollection.getAnalysisTimestamp())
				.setSessionId(sessionId)
				.addAllMyIssueHashes(bugsByHash.keySet())
				.build();
		OutputStream stream = conn.getOutputStream();
		hashList.writeTo(stream);
		stream.close();
		if (conn.getResponseCode() != 200) {
			throw new IOException("Response code " + conn.getResponseCode()
					+ " : " + conn.getResponseMessage());
		}
		LogInResponse response = LogInResponse.parseFrom(conn.getInputStream());
		conn.disconnect();
		return response;
	}

	/** package-private for testing */
	void uploadIssues(Collection<BugInstance> bugsToSend)
			throws MalformedURLException, IOException {
		UploadIssues.Builder issueList = UploadIssues.newBuilder();
		issueList.setSessionId(sessionId);
		for (BugInstance bug: bugsToSend) {
			issueList.addNewIssues(ProtoClasses.Issue.newBuilder()
					.setHash(bug.getInstanceHash())
					.setBugPattern(bug.getType())
					.setPriority(bug.getPriority())
					.setPrimaryClass(bug.getPrimaryClass().getClassName())
					.setFirstSeen(bug.getFirstVersion())
					.setLastSeen(bug.getLastVersion())
					.build());
		}
		openUrl(issueList.build(), "/upload-issues");

	}

	/** package-private for testing */
	HttpURLConnection openConnection(String url)
			throws IOException, MalformedURLException {
		URL u = new URL(AppEngineNameLookup.HOST + url);
		return (HttpURLConnection) u.openConnection();
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

	private void openUrl(GeneratedMessage uploadMsg, String url) {
		try {
			HttpURLConnection conn = openConnection(url);
			conn.setDoOutput(true);
			conn.connect();
			try {
				OutputStream stream = conn.getOutputStream();
				uploadMsg.writeTo(stream);
				stream.close();
				if (conn.getResponseCode() != 200) {
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

}
