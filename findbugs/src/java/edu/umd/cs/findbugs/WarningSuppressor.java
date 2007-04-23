
package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.filter.Matcher;

abstract public class WarningSuppressor implements Matcher {

	final static boolean DEBUG = false;

	String bugPattern;

	public WarningSuppressor(String bugPattern) {
		this.bugPattern = bugPattern;
		if (DEBUG)
		System.out.println("Suppressing " + bugPattern);
		}

	public boolean match(BugInstance bugInstance) {

		if (DEBUG) {
	System.out.println("Checking " + bugInstance);
	System.out.println("    type:" + bugInstance.getType());
	System.out.println(" against: " + bugPattern);

	}
	if (!(
		bugPattern == null
		|| bugInstance.getType().startsWith(bugPattern)
		|| bugInstance.getBugPattern().getCategory().equalsIgnoreCase(bugPattern)
	    || bugInstance.getBugPattern().getAbbrev().equalsIgnoreCase(bugPattern)))
	return false;
	if (DEBUG)
	System.out.println(" pattern matches");
	return true;
	}
}

