package edu.umd.cs.findbugs.gui2;
import java.util.HashSet;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;


/**
 * Holds a bunch of matchers, and only matches a bug if all
 * the submatchers match it.  Matchetymatch.
 */
public class CompoundMatcher extends HashSet<Matcher> implements Matcher
{
	private static final long serialVersionUID = -6167545252176658833L;

	public boolean match(BugInstance bugInstance)
	{
		for (Matcher i : this)
			if (!i.match(bugInstance))
				return false;
		return true;
	}
}
