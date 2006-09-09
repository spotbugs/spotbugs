package edu.umd.cs.findbugs.gui2;

import java.util.ArrayList;
import java.util.HashSet;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;

/**
 * A list of all bugs to filter out, only we call it suppressing them
 */
public class SuppressionMatcher extends ArrayList<BugInstance> implements Matcher
{
	private static final long serialVersionUID = -689204051024507484L;

	public boolean match(BugInstance bugInstance)
	{
		return (!contains(bugInstance));
	}

	public boolean add(BugInstance bugInstance)
	{
		if (contains(bugInstance))
			return false;
		return super.add(bugInstance);
	}
}