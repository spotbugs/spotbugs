package edu.umd.cs.findbugs.gui2;

import java.io.Serializable;
import java.util.HashSet;

import javax.swing.tree.TreePath;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;

/**
 * Why this wasn't just called Filter is still somewhat of a mystery.
 * FilterMatchers are Filters, pass in a StringPair like Priority, High
 * and all the high priority bugs disappear, Its that easy.
 * 
 *
 */
public class FilterMatcher implements Matcher, Serializable, Comparable<FilterMatcher>
{
	private static final long serialVersionUID = -4859486064351510016L;	
	private Sortables filterBy;
	private String value;
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
	
	public FilterMatcher(Sortables filterBy, String value)
	{
		this.filterBy = filterBy;
		this.value = value;
		this.active = true;
	}
	
	public void setActive(boolean active)
	{
		if (active != this.active)
		{
			this.active = active;
			if (active==true)
				notifyListeners(FilterListener.FILTERING, null);
			else
				notifyListeners(FilterListener.UNFILTERING, null);
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
		
		return !filterBy.getFrom(bugInstance).equals(value);
	}
	
	public String toString()
	{
		return filterBy.toString() + " is " + filterBy.formatValue(value);
	}
	
	public static boolean addFilterListener(FilterListener newListener)
	{
		return listeners.add(newListener);
	}
	
	public static void removeFilterListener(FilterListener toRemove)
	{
		listeners.remove(toRemove);
	}
	
	public static void notifyListeners(int whatsGoingOnCode, TreePath optionalPath)
	{
		HashSet<FilterListener> listeners = (HashSet<FilterListener>)FilterMatcher.listeners.clone();
		if (whatsGoingOnCode==FilterListener.FILTERING || whatsGoingOnCode== FilterListener.UNFILTERING)
			for (FilterListener i : listeners)
				i.clearCache();
		else if (whatsGoingOnCode==FilterListener.SUPPRESSING && optionalPath!=null)
			for (FilterListener i : listeners)
				i.suppressBug(optionalPath);
		else if (whatsGoingOnCode==FilterListener.UNSUPPRESSING && optionalPath!=null)
			for (FilterListener i : listeners)
				i.unsuppressBug(optionalPath);
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof FilterMatcher))
			return false;
		if (filterBy.equals(((FilterMatcher)o).filterBy) && value.equals(((FilterMatcher)o).value))
			return true;
		return false;
	}
	
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
}
