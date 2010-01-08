package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.db.AppEngineNameLookup;

public class AppEngineCloud extends AbstractCloud {

	// private static final String HOST = "http://theflybush.appspot.com";
	private static final String HOST = "http://localhost:8080";

	private Map<String, Issue> issuesByHash = new HashMap<String, Issue>();
	private String user;

	private String findbugsUser;

	public AppEngineCloud(BugCollection bugs) {
		super(bugs);

	}

	/** package-private for testing */
	void setUsername(String user) {
		this.user = user;
	}

	public boolean availableForInitialization() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean initialize() {
		findbugsUser = new AppEngineNameLookup().getUserName(bugCollection);
		
		if (findbugsUser == null)
			return false;
		bugsPopulated();
		return true;
	}

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
				bugsByHash.remove(issue.getHash());
			}
			uploadIssues(bugsByHash.values());

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private @CheckForNull LogInResponse submitHashes(Map<String, BugInstance> bugsByHash)
			throws IOException, MalformedURLException {
		HttpURLConnection conn = openConnection("/find-issues");
		conn.setDoOutput(true);
		conn.connect();
		LogIn hashList = LogIn.newBuilder()
				.setAnalysisTimestamp(bugCollection.getAnalysisTimestamp())
				.setSessionId(0)
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
		HttpURLConnection conn = openConnection("/upload-issues");
		conn.setDoOutput(true);
		conn.connect();
		OutputStream stream = conn.getOutputStream();
		issueList.build().writeTo(stream);
		stream.close();
	}

	/** package-private for testing */
	HttpURLConnection openConnection(String url)
			throws IOException, MalformedURLException {
		URL u = new URL(HOST + url);
		return (HttpURLConnection) u.openConnection();
	}


	public String getUser() {
		return user;
	}

	public void bugFiled(BugInstance b, Object bugLink) {
		throw new UnsupportedOperationException();
	}

	public long getUserTimestamp(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		if (e == null) return Long.MAX_VALUE;
		return e.getWhen();
	}

	public void setUserTimestamp(BugInstance b, long timestamp) {
		throw new UnsupportedOperationException();
	}

	public UserDesignation getUserDesignation(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		if (e == null)
			return UserDesignation.UNCLASSIFIED;
		return UserDesignation.valueOf(e.getDesignation());
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

	public void setUserDesignation(BugInstance b, UserDesignation u,
			long timestamp) {
		throw new UnsupportedOperationException();

	}

	public String getUserEvaluation(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		if (e == null) return null;
		return e.getComment();
	}

	public void setUserEvaluation(BugInstance b, String e, long timestamp) {
		throw new UnsupportedOperationException();
	}

	public long getFirstSeen(BugInstance b) {
		Issue issue = issuesByHash.get(b.getInstanceHash());
		if (issue == null)
			return Long.MAX_VALUE;
		return issue.getFirstSeen();

	}

	public void storeUserAnnotation(BugInstance bugInstance) {
		throw new UnsupportedOperationException();
	}

}
