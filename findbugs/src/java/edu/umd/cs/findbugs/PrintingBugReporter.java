package edu.umd.cs.findbugs;

import java.util.*;

/**
 * A simple BugReporter which simply prints the formatted message
 * to System.out.
 */
public class PrintingBugReporter implements BugReporter {
	private HashSet<BugInstance> seenAlready = new HashSet<BugInstance>();

	public void reportBug(BugInstance bugInstance) {
		if (!seenAlready.contains(bugInstance)) {
			System.out.println(bugInstance.getMessage());
			seenAlready.add(bugInstance);
		}
	}

	public void finish() { }
}

// vim:ts=4
