package edu.umd.cs.findbugs.cloud.appEngine;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;

public class Issue {
	Issue() {}
	Issue (BugInstance bug) {}
	
	String hash;
	String bugPattern;
	int priority;
	String primaryClass;
	long firstSeen, lastSeen;
	List<Evaluation> evaluations;

}
