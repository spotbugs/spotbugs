package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.BugAnnotationVisitor;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.HashList;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.IssueList;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.IssueList.Builder;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class AppEngineCloud extends AbstractCloud {

	private Map<String, Issue> issuesByHash = new HashMap<String, Issue>();
	private String user;

	protected AppEngineCloud(BugCollection bugs) {
		super(bugs);

	}

	/** package-private for testing */
	void setUsername(String user) {
		this.user = user;
	}
	
	@Override
	public boolean availableForInitialization() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean initialize() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void bugsPopulated() {
		Map<String, BugInstance> bugsByHash = new HashMap<String, BugInstance>();

		for(BugInstance b : bugCollection.getCollection()) {
			bugsByHash.put(b.getInstanceHash(), b);
		}

		// send all instance hashes to server
		try {
			IssueList response = submitHashes(bugsByHash);
			for (Issue issue : response.getFoundIssuesList()) {
				issuesByHash.put(issue.getHash(), issue);
			}
			List<String> missingHashes = response.getMissingIssuesList();
			bugsByHash.keySet().retainAll(missingHashes);
			sendIssues(bugsByHash.values());

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private @CheckForNull IssueList submitHashes(Map<String, BugInstance> bugsByHash)
			throws IOException, MalformedURLException {
		HttpURLConnection conn = openConnection("/find-issues");
		conn.connect();
		HashList hashList = HashList.newBuilder().addAllHashes(bugsByHash.keySet()).build();
		OutputStream stream = conn.getOutputStream();
		hashList.writeTo(stream);
		stream.close();
		if (conn.getResponseCode() != 200) {
			// TODO error
			return null;
		}
		IssueList response = IssueList.parseFrom(conn.getInputStream());
		conn.disconnect();
		return response;
	}

	/** package-private for testing */
	void sendIssues(Collection<BugInstance> bugsToSend) throws MalformedURLException, IOException {
		IssueList.Builder issueList = IssueList.newBuilder();
		for (BugInstance bug: bugsToSend) {
			issueList.addFoundIssues(ProtoClasses.Issue.newBuilder()
					.setHash(bug.getInstanceHash())
					.setBugPattern(bug.getType())
					.setPriority(bug.getPriority())
					.setRank(bug.getBugRank())
					.setPrimaryClass(bug.getPrimaryClass().getClassName())
					.setFirstSeen(bug.getFirstVersion())
					.setLastSeen(bug.getLastVersion())
					.build());
		}
		HttpURLConnection conn = openConnection("/upload-issues");
		conn.connect();
		OutputStream stream = conn.getOutputStream();
		issueList.build().writeTo(stream);
		stream.close();
	}

	/** package-private for testing */
	HttpURLConnection openConnection(String url)
			throws IOException, MalformedURLException {
		return (HttpURLConnection) new URL("http://localhost:8080" + url).openConnection();
	}


	@Override
	public String getUser() {
		return user;
	}

	@Override
	public void bugFiled(BugInstance b, Object bugLink) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getUserTimestamp(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		if (e == null) return Long.MAX_VALUE;
		return e.getWhen();
	}

	@Override
	public void setUserTimestamp(BugInstance b, long timestamp) {
		throw new UnsupportedOperationException();
	}

	@Override
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

	@Override
	public void setUserDesignation(BugInstance b, UserDesignation u,
			long timestamp) {
		throw new UnsupportedOperationException();

	}

	@Override
	public String getUserEvaluation(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		if (e == null) return null;
		return e.getComment();
	}

	@Override
	public void setUserEvaluation(BugInstance b, String e, long timestamp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getFirstSeen(BugInstance b) {
		Issue issue = issuesByHash.get(b.getInstanceHash());
		if (issue == null)
			return Long.MAX_VALUE;
		return issue.getFirstSeen();

	}

	@Override
	public void storeUserAnnotation(BugInstance bugInstance) {
		throw new UnsupportedOperationException();
	}

}
