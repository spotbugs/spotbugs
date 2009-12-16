package edu.umd.cs.findbugs.cloud.appEngine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.util.MultiMap;

public class AppEngineCloud extends AbstractCloud {

	protected AppEngineCloud(BugCollection bugs) {
		super(bugs);
		
	}

	@Override
	public boolean availableForInitialization() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean initialize() {
		// TODO Auto-generated method stub
		return false;
	}

	HashMap<String, Issue> issueMap = new HashMap<String, Issue>();
	@Override
	public void bugsPopulated() {
		Map<String, BugInstance> hashes 
		= new HashMap<String, BugInstance>();
		
		for(BugInstance b : bugCollection.getCollection())
			hashes.put(b.getInstanceHash(), b);
		
		// send all instance hashes to server
		// sent hashes.keySet()
		
		for(;;) {
			Issue issue = null; // get instance from server
			// evaluations come with it
			if (issue == null) 
				break;
			issueMap.put(issue.hash, issue);
			hashes.remove(issue.hash);
		}
		for(Map.Entry<String, BugInstance> e: hashes.entrySet()) {
			Issue issue = new Issue(e.getValue());
			// send issue to server
		}
		
	}


	@Override
	public String getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void bugFiled(BugInstance b, Object bugLink) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getUserTimestamp(BugInstance b) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setUserTimestamp(BugInstance b, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UserDesignation getUserDesignation(BugInstance b) {
		Evaluation e = getMostRecentEvaluation(b);
		if (e == null)
			return UserDesignation.UNCLASSIFIED;
		return UserDesignation.valueOf(e.designation);
	}

	
	public Evaluation getMostRecentEvaluation(BugInstance b) {
		Issue issue = issueMap.get(b.getInstanceHash());
		if (issue == null)
			return null;
		Evaluation mostRecent = null;
		long when = Long.MIN_VALUE;
		for(Evaluation e : issue.evaluations) 
			if (e.who.equals(getUser()) && e.when > when) 
				mostRecent = e;
			
		return mostRecent;
	}
	@Override
	public void setUserDesignation(BugInstance b, UserDesignation u,
			long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUserEvaluation(BugInstance b) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserEvaluation(BugInstance b, String e, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getFirstSeen(BugInstance b) {
		Issue issue = issueMap.get(b.getInstanceHash());
		if (issue == null)
			return Long.MAX_VALUE;
		return issue.firstSeen;
			
	}

	@Override
	public void storeUserAnnotation(BugInstance bugInstance) {
		// TODO Auto-generated method stub
		
	}

}
