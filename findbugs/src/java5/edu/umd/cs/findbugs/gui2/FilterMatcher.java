/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.lang.RuntimeException;
import javax.swing.tree.TreePath;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Why this wasn't just called Filter is still somewhat of a mystery.
 * FilterMatchers are Filters, pass in a StringPair like Priority, High
 * and all the high priority bugs disappear, Its that easy.
 */
public class FilterMatcher implements Matcher, Serializable, Comparable<FilterMatcher>
{
	enum FilterWhere {FILTER_EXACTLY, FILTER_AT_OR_AFTER, FILTER_AT_OR_BEFORE, FILTER_ALL_BUT};
	private static final long serialVersionUID = -4859486064351510016L;

	private Sortables filterBy;
	private String value;
	private FilterWhere mode;
	protected boolean active;
	private static HashSet<FilterListener> listeners = new HashSet<FilterListener>();

	public FilterMatcher(StringPair sp)
	{
		this(sp.key, sp.value);
	}

	Sortables getFilterBy()
	{
		return filterBy;
	}

	String getValue()
	{
		return value;
	}

	public FilterMatcher(Sortables filterBy, String value, FilterWhere mode) //0 = exactly; 1 = at or after; 2 = at or before; 3 = not at
	{
		this.filterBy = filterBy;
		this.value = value;
		this.mode = mode;
		this.active = true;
	}

	public FilterMatcher(Sortables filterBy, String value)
	{
		this.filterBy = filterBy;
		this.value = value;
		this.mode = FilterWhere.FILTER_EXACTLY;
		this.active = true;
	}
	public void setActive(boolean active)
	{
		if (active != this.active)
		{
			this.active = active;
			if (active==true)
				notifyListeners(FilterListener.Action.FILTERING, null);
			else
				notifyListeners(FilterListener.Action.UNFILTERING, null);
		}
	}

	public boolean isActive()
	{
		return active;
	}

	public boolean match(BugInstance bugInstance)
	{		
		if (!active)
			return true;

		SortableStringComparator ssc = new SortableStringComparator(filterBy);
		switch(mode)
		{
		case FILTER_EXACTLY: return (ssc.compare(filterBy.getFrom(bugInstance), value) != 0);
		case FILTER_AT_OR_AFTER: return (ssc.compare(filterBy.getFrom(bugInstance), value) < 0);
		case FILTER_AT_OR_BEFORE: return (ssc.compare(filterBy.getFrom(bugInstance), value) > 0);
		case FILTER_ALL_BUT: return (ssc.compare(filterBy.getFrom(bugInstance), value) == 0);
		default: return true;
		}
	}

	@Override
	public String toString()
	{
		switch(mode)
		{
		case FILTER_EXACTLY: return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " " + edu.umd.cs.findbugs.L10N.getLocalString("mode.equal_to", "equal to") + " " + filterBy.formatValue(value);
		case FILTER_AT_OR_AFTER: return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " " + edu.umd.cs.findbugs.L10N.getLocalString("mode.at_or_after", "at or after") + " " + filterBy.formatValue(value);
		case FILTER_AT_OR_BEFORE: return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " " + edu.umd.cs.findbugs.L10N.getLocalString("mode.at_or_before", "at or before") + " " + filterBy.formatValue(value);
		case FILTER_ALL_BUT: return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " " + edu.umd.cs.findbugs.L10N.getLocalString("mode.not_equal_to", "not equal to") + " " + filterBy.formatValue(value);
		default: throw new RuntimeException();
		}
	}

	public static boolean addFilterListener(FilterListener newListener)
	{
		return listeners.add(newListener);
	}

	public static void removeFilterListener(FilterListener toRemove)
	{
		listeners.remove(toRemove);
	}

	public static void notifyListeners(FilterListener.Action whatsGoingOnCode,
			TreePath optionalPath) {
		HashSet<FilterListener> listeners = (HashSet<FilterListener>) FilterMatcher.listeners
				.clone();
		switch (whatsGoingOnCode) {
		case FILTERING:
		case UNFILTERING:
			for (FilterListener i : listeners)
				i.clearCache();
			break;
		case SUPPRESSING:
			for (FilterListener i : listeners)
				i.suppressBug(optionalPath);
			break;
		case UNSUPPRESSING:
			for (FilterListener i : listeners)
				i.unsuppressBug(optionalPath);
			break;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof FilterMatcher))
			return false;
		if (filterBy.equals(((FilterMatcher)o).filterBy) && value.equals(((FilterMatcher)o).value))
			return true;
		return false;
	}

	@Override
	public int hashCode()
	{
		return value.hashCode() + filterBy.hashCode();
	}

	public int compareTo(FilterMatcher that)
	{
		if (this.filterBy != that.filterBy)
			return (this.filterBy.ordinal() < that.filterBy.ordinal() ? -1 : 1);

		return this.value.compareTo(that.value);
	}
	
	 public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		    throw new UnsupportedOperationException(); 
	    }
}
