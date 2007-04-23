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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.TreeModelEvent;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugTreeModel.BranchOperationException;

/**
 * Filter out bugs which fail (match) all filters.
 * This is what happens when you filter out a branch.
 */
public class StackedFilterMatcher extends FilterMatcher
{
	private static final long serialVersionUID = 3958267780332359162L;
	private FilterMatcher[] filters;

	@Override
	Sortables getFilterBy()
	{
		throw new UnsupportedOperationException("Stacked filter matchers do not filter out a single Sortables, use getFilters()");
	}

	@Override
	String getValue()
	{
		throw new UnsupportedOperationException("Stacked filter matchers do not filter out a single Sortables's value, use getFilters and getValue individually on returned filters.");
	}

	public StackedFilterMatcher(FilterMatcher... filters)
	{
		super(null, null);
		this.filters = filters;
	}

	//If only FilterMatcher's setActive were as simple as this one... not.
	//See BugTreeModel's long ranting comment about filtering to see the reason for all this
	//All this does is not force the tree to rebuild when you turn filters for branches on and off
	@Override
	public void setActive(boolean active)
	{
		TreeModelEvent event=null;
		BugTreeModel.TreeModification whatToDo;


		if (active != this.active)
		{
			if (active==false)
				this.active=active;

			StackedFilterMatcher theSame= this;
			FilterMatcher[] filtersInStack=theSame.getFilters();
			ArrayList<Sortables> order=MainFrame.getInstance().getSorter().getOrder();
			int sizeToCheck=filtersInStack.length;
			if (order.contains(Sortables.DIVIDER))
				if (order.indexOf(Sortables.DIVIDER) < filtersInStack.length)
				{
					sizeToCheck++;
				}
			List<Sortables> sortablesToCheck=order.subList(0, Math.min(sizeToCheck, order.size()));
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
			ArrayList<String> finalPath=new ArrayList<String>();
			for (int x=0;x<almostPath.size();x++)
			{
				Sortables s=almostPathSortables.get(x);
				if (MainFrame.getInstance().getSorter().getOrderBeforeDivider().contains(s))
					finalPath.add(almostPath.get(x));
			}
			try {
				if (finalPath.size()==filtersInStack.length)
				{
					if (active==true)
					{
						event=((BugTreeModel)(MainFrame.getInstance().getTree().getModel())).removeBranch(finalPath);
						whatToDo=BugTreeModel.TreeModification.REMOVE;
					}
					else
					{
						event=((BugTreeModel)(MainFrame.getInstance().getTree().getModel())).insertBranch(finalPath);
						whatToDo=BugTreeModel.TreeModification.INSERT;
					}
				}
				else 
				{
					event=((BugTreeModel)(MainFrame.getInstance().getTree().getModel())).restructureBranch(finalPath,active);//if active is true, this removes, if active if false, it inserts
					if (active) whatToDo=BugTreeModel.TreeModification.REMOVERESTRUCTURE;
					else whatToDo=BugTreeModel.TreeModification.INSERTRESTRUCTURE;
				}

			if (active==true)
				this.active=active;
			((BugTreeModel)(MainFrame.getInstance().getTree().getModel())).sendEvent(event,whatToDo);		
			}
			catch (BranchOperationException e)
			{
				//Another filter already filters out the branch this filter would filter out, set active, but dont send any tree model events.
				this.active=active;
			}
		}

	}

	public FilterMatcher[] getFilters()
	{
		return filters;
	}

	@Override
	public boolean match(BugInstance bugInstance)
	{
		if (!isActive())
			return true;

		for (FilterMatcher i : filters)
			if (i.match(bugInstance))
				return true;

		return false;
	}

	@Override
	public String toString()
	{
		//return "StackedFilterMatcher: " + Arrays.toString(filters);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < filters.length - 1; i++)
			result.append(filters[i].toString() + (i == filters.length - 2 ? " " : ", "));
		if (filters.length > 1)
			result.append("and ");
		if (filters.length > 0)
			result.append(filters[filters.length - 1]);
		return result.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof StackedFilterMatcher))
			return false;

		FilterMatcher[] mine = new FilterMatcher[filters.length];
		System.arraycopy(this.filters, 0, mine, 0, mine.length);
		Arrays.sort(mine);

		FilterMatcher[] others = new FilterMatcher[((StackedFilterMatcher)o).filters.length];
		System.arraycopy(((StackedFilterMatcher)o).filters, 0, others, 0, others.length);
		Arrays.sort(others);

		return (Arrays.equals(mine, others));
	}

	@Override
	public int hashCode()
	{
		return filters.hashCode();
	}

	public static void main(String[] args)
	{
		System.out.println(new StackedFilterMatcher(new FilterMatcher[0]).equals(new StackedFilterMatcher(new FilterMatcher[0])));
	}
}
