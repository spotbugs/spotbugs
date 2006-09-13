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

/**
 * These are the branches in our tree, each branch forms a complete query that could be sent to the main bugset to return all the bugs it contains
 * For example, a single bugAspects could be <priority,high> or it could be <priority,high>,<designation,must fix>,<class,fishpond>,<package,default> 
 * 
 * In this implementation, <priority,high>,<designation,unclassified> is different from <designation,unclassified>,<priority,high>.  (I'm not talking about the fact we use the .equals from ArrayList, I'm talking about what a query would return, though both are true)
 * For a speed boost, this class could be rewritten to make these equal, BugSet could be rewritten to cache full queries off the main BugSet, (instead of caching each part of the query separately in the BugSets created)
 * and resetData could be rewritten to work more like Swing's validate, only clearing data if the data is wrong.  This would save time after changing certain aspects of the tree. 
 * Just an idea, I wouldn't suggest it unless its absolutely necessary. -Dan
 * 
 * 
 * @author All of us
 */
public class BugAspects extends ArrayList<BugAspects.StringPair>
{
	private static final long serialVersionUID = -5503915081879996968L;
	private int countOfPeople=-1;
	
	public String toString()
	{
		if (size() == 0)
			return "Bugs (" + countOfPeople + ")";
		else	
		{
			if (countOfPeople==-1)
				return get(size() - 1).value;
			else
				return get(size() - 1).key.formatValue(get(size() -1).value) + " (" + countOfPeople + ")";
		}
	}
	
	/**
	 * This is how the numbers after the branches contain the number of bugs in them, even if they aren't the final branch
	 * @param count
	 */
	public void setCount(int count)
	{
		countOfPeople=count;
	}
	
	public int getCount()
	{
		return countOfPeople;
	}
	
	public BugAspects()
	{
		super();
	}
	
	public BugAspects(BugAspects a)
	{
		super(a);
		countOfPeople = a.countOfPeople;
	}
	
	public BugAspects addToNew(StringPair sp)
	{
		BugAspects result = new BugAspects(this);//(BugAspects)this.clone();
		result.add(sp);
		return result;
	}
	
	static class StringPair
	{
		public Sortables key;
		public String value;
		
		public StringPair() {}
		public StringPair(Sortables key, String value)
		{
			this.key = key;
			this.value = value;
		}
		
		public int hashCode()
		{
			return key.hashCode() + value.hashCode();
		}
		
		public boolean equals(Object that)
		{
			if (that == null || !(that instanceof StringPair))
				return false;
			return this.key.equals(((StringPair)that).key) && this.value.equals(((StringPair)that).value);
		}
		
		public String toString()
		{
			return key +":"+ value;
		}
	}
}
