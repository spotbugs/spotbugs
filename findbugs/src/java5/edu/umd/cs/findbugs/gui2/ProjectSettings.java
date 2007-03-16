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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.gui2.BugTreeModel.BranchOperationException;

/**
 * This is the .fas file stored when projects are saved
 * All project related information goes here.  Anything that would be shared between multiple projects goes into GUISaveState instead
 */
public class ProjectSettings implements Serializable
{	
	private static final long serialVersionUID = 6505872267795979672L;

	// Singleton
	private ProjectSettings() 
	{
		allMatchers = new CompoundMatcher();
		suppressionMatcher = new SuppressionMatcher();
		filters = new ArrayList<FilterMatcher>();
		allMatchers.add(suppressionMatcher);
	}
	private static ProjectSettings instance;
	public static ProjectSettings newInstance()
	{
		instance = new ProjectSettings();
		DeadBugFilter dbf=new DeadBugFilter(Sortables.LASTVERSION, "-1", FilterMatcher.FilterWhere.FILTER_ALL_BUT);
		//Important: add the deadbug filter directly to filters and allmatchers, dont go through addFilter, otherwise it causes a
		//tree to rebuild.
		instance.filters.add(dbf);
		instance.allMatchers.add(dbf);
		PreferencesFrame.getInstance().updateFilterPanel();
		PreferencesFrame.getInstance().clearSuppressions();
		return instance;
	}
	public static ProjectSettings getInstance()
	{
		if (instance == null)
			instance= new ProjectSettings();
		return instance;
	}
	
	/**
	 * The matcher suppressing all user-selected bugs.
	 */
	private SuppressionMatcher suppressionMatcher;
	
	/**
	 * The list of all defined filters
	 */
	private ArrayList<FilterMatcher> filters;
	
	/**
	 * The CompoundMatcher enveloping all enabled matchers.
	 */
	private CompoundMatcher allMatchers;
		
	/**
	 * Max number of previous comments stored.
	 */
	private int maxSizeOfPreviousComments;
		
	public static void loadInstance(InputStream in)
	{
		try
		{
			instance = (ProjectSettings) new ObjectInputStream(in).readObject();
			PreferencesFrame.getInstance().updateFilterPanel();
			for (BugInstance bug: instance.getSuppressionMatcher())
			{
				PreferencesFrame.getInstance().suppressionsChanged(new BugLeafNode(bug));
			}
            in.close();
		}
		catch (ClassNotFoundException e)
		{
			if (MainFrame.DEBUG) System.err.println("Error in deserializing Settings:");
			Debug.println(e);
		}
		catch (IOException e)
		{
			if (MainFrame.DEBUG) System.err.println("IO error in deserializing Settings:");
			Debug.println(e);
			instance=newInstance();
		}
	}
	
	public void save(OutputStream out)
	{
		try
		{
			new ObjectOutputStream(out).writeObject(this);
		}
		catch (IOException e)
		{
			if (MainFrame.DEBUG) System.err.println("Error serializing Settings:");
			Debug.println(e);
		}
	}
	
	SuppressionMatcher getSuppressionMatcher()
	{
		return suppressionMatcher;
	}
	
	void setSuppressionMatcher(SuppressionMatcher suppressionMatcher)
	{
		this.suppressionMatcher = suppressionMatcher;
	}
	
	CompoundMatcher getAllMatchers()
	{
		return allMatchers;
	}
	
	public void addFilter(FilterMatcher filter)
	{
		filters.add(filter);
		allMatchers.add(filter);
		if (!(filter instanceof StackedFilterMatcher))
			FilterMatcher.notifyListeners(FilterListener.Action.FILTERING,null);
		else 
		{
			StackedFilterMatcher theSame= (StackedFilterMatcher) filter;
			FilterMatcher[] filtersInStack=theSame.getFilters();
			ArrayList<Sortables> order=MainFrame.getInstance().getSorter().getOrder();
			int sizeToCheck=filtersInStack.length;
			List<Sortables> sortablesToCheck=order.subList(0, sizeToCheck);
			Debug.println("Size to check" + sizeToCheck + " checking list" + sortablesToCheck);
			Debug.println("checking filters");
			ArrayList<String> almostPath=new ArrayList<String>();
			ArrayList<Sortables> almostPathSortables=new ArrayList<Sortables>();
			for (int x=0; x< sortablesToCheck.size();x++)
			{
				Sortables s=sortablesToCheck.get(x);
				for (FilterMatcher fm:filtersInStack)
				{
					if (s.equals(fm.getFilterBy()))
					{
						almostPath.add(fm.getValue());
						almostPathSortables.add(fm.getFilterBy());
					}
				}
			}
			if (almostPath.size()==filtersInStack.length)
			{
				ArrayList<String> finalPath=new ArrayList<String>();
				for (int x=0;x<almostPath.size();x++)
				{
					Sortables s=almostPathSortables.get(x);
					if (MainFrame.getInstance().getSorter().getOrderBeforeDivider().contains(s))
						finalPath.add(almostPath.get(x));
				}
				BugTreeModel model=((BugTreeModel)(MainFrame.getInstance().getTree().getModel()));
				try {
					model.sendEvent(model.removeBranch(finalPath), BugTreeModel.TreeModification.REMOVE);
				}
				catch (BranchOperationException e)
				{
					throw new IllegalStateException("They added a stacked filter on a branch that doesn't exist... Whaa?");
				}
			}
			else
			{
				FilterMatcher.notifyListeners(FilterListener.Action.FILTERING,null);
				throw new IllegalStateException("What huh?  How'd they add a stacked filter matcher bigger than the number of branches in the tree?!");
			}
		}
		PreferencesFrame.getInstance().updateFilterPanel();
		MainFrame.getInstance().updateStatusBar();
	}
	
	public void addFilters(FilterMatcher[] newFilters)
	{
		for (FilterMatcher i : newFilters)
			if (!filters.contains(i))
			{
				filters.add(i);
				allMatchers.add(i);
			}
			else //if filters contains i
			{
				filters.get(filters.indexOf(i)).setActive(true);
				//FIXME Do I need to do this for allMatchers too?  Or are the filters all the same, with both just holding references?
			}
		FilterMatcher.notifyListeners(FilterListener.Action.FILTERING, null);
		PreferencesFrame.getInstance().updateFilterPanel();
		MainFrame.getInstance().updateStatusBar();
	}
	
	public boolean removeFilter(FilterMatcher filter)
	{
		boolean result = filters.remove(filter) && allMatchers.remove(filter);
		FilterMatcher.notifyListeners(FilterListener.Action.UNFILTERING,null);
		PreferencesFrame.getInstance().updateFilterPanel();
		MainFrame.getInstance().updateStatusBar();
		return result;
	}
	
	ArrayList<FilterMatcher> getAllFilters()
	{
		return filters;
	}
	
	/**
	 * @return Returns the maximum number of previous comments stored.
	 */
	public int getMaxSizeOfPreviousComments(){
		return maxSizeOfPreviousComments;
	}
	
	/**
	 * Sets the maximum number of previous comments stored.
	 * @param num
	 */
	public void setMaxSizeOfPreviousComments(int num){
		maxSizeOfPreviousComments = num;
	}
}