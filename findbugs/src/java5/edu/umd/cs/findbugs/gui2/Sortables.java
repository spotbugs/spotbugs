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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;
import edu.umd.cs.findbugs.gui.L10N;

/**
 * A useful enum for dealing with all the types of filterable and sortable data in BugInstances
 * This is the preferred way for getting the information out of a BugInstance and formatting it for display
 * It also has the comparators for the different types of data
 * 
 * @author Reuven
 */
public enum Sortables implements Comparator<StringPair>
{
	PRIORITY(L10N.getLocalString("sort.priority", "Priority"))
	{
		public String getFrom(BugInstance bug)
		{
			return String.valueOf(bug.getPriority());
		}
		
		@Override
		public String formatValue(String value)
		{
			if (value.equals(String.valueOf(Detector.HIGH_PRIORITY)))
				return edu.umd.cs.findbugs.gui.L10N.getLocalString("sort.priority_high", "High");
			if (value.equals(String.valueOf(Detector.NORMAL_PRIORITY)))
				return edu.umd.cs.findbugs.gui.L10N.getLocalString("sort.priority_normal", "Normal");
			if (value.equals(String.valueOf(Detector.LOW_PRIORITY)))
				return edu.umd.cs.findbugs.gui.L10N.getLocalString("sort.priority_low", "Low");
			if (value.equals(String.valueOf(Detector.EXP_PRIORITY)))
				return edu.umd.cs.findbugs.gui.L10N.getLocalString("sort.priority_experimental", "Experimental");
			return edu.umd.cs.findbugs.gui.L10N.getLocalString("sort.priority_ignore", "Ignore"); // This probably shouldn't ever happen, but what the hell, let's be complete
			
		}
		
		@Override
		public int compare(StringPair one, StringPair two)
		{
			// Numerical
			return Integer.valueOf(one.value).compareTo(Integer.valueOf(two.value));
		}
	},
	CLASS(L10N.getLocalString("sort.class", "Class"))
	{
		public String getFrom(BugInstance bug)
		{
			return bug.getPrimarySourceLineAnnotation().getClassName();
		}
				
		@Override
		public int compare(StringPair one, StringPair two)
		{
			// If both have dollar signs and are of the same outer class, compare the numbers after the dollar signs.
			try
			{
				if (one.value.contains("$") && two.value.contains("$")
						&& one.value.substring(0, one.value.lastIndexOf("$")).equals(two.value.substring(0, two.value.lastIndexOf("$"))))
					return Integer.valueOf(one.value.substring(one.value.lastIndexOf("$"))).compareTo(Integer.valueOf(two.value.substring(two.value.lastIndexOf("$"))));
			}
			catch (NumberFormatException e) {} // Somebody's playing silly buggers with dollar signs, just do it lexicographically
			
			// Otherwise, lexicographicalify it
			return one.value.compareTo(two.value);
		}
	},
	PACKAGE(L10N.getLocalString("sort.package", "Package"))
	{
		public String getFrom(BugInstance bug)
		{
			return bug.getPrimarySourceLineAnnotation().getPackageName();
		}
		
		public String formatValue(String value)
		{
			if (value.equals(""))
				return "(Default)";
			return value;
		}
	},
	CATEGORY(L10N.getLocalString("sort.category", "Category"))
	{
		public String getFrom(BugInstance bug)
		{
			return bug.getBugPattern().getCategory();
		}
		
		@Override
		public String formatValue(String value)
		{			
			return I18N.instance().getBugCategoryDescription(value);
		}
	},
	DESIGNATION(L10N.getLocalString("sort.designation", "Designation"))
	{
		public String getFrom(BugInstance bug)
		{
			return bug.getUserDesignationKey();
		}
						
		/**
		 * value is the key of the designations.
		 * @param value
		 * @return
		 */
		@Override
		public String formatValue(String value)
		{
			return I18N.instance().getUserDesignation(value);
		}
		
		public String[] getAllSorted()
		{//FIXME I think we always want user to see all possible designations, not just the ones he has set in his project, Agreement?  -Dan
			List<String> sortedDesignations=I18N.instance().getUserDesignationKeys(true);
			return sortedDesignations.toArray(new String[sortedDesignations.size()]);
		}
	},
	BUGCODE(L10N.getLocalString("sort.bug_kind", "Bug Kind"))
	{
		public String getFrom(BugInstance bug)
		{
			return bug.getBugPattern().getAbbrev();
		}
				
		public String formatValue(String value)
		{
			return I18N.instance().getBugTypeDescription(value);
		}
		
		@Override
		public int compare(StringPair one, StringPair two)
		{
			return formatValue(one.value).compareTo(formatValue(two.value));
		}
	},
	TYPE(L10N.getLocalString("sort.bug_pattern", "Bug Pattern"))
	{
		public String getFrom(BugInstance bug)
		{
			return bug.getBugPattern().getType();
		}
				
		public String formatValue(String value)
		{
			return I18N.instance().getShortMessageWithoutCode(value);
		}
	},
	DIVIDER(" ")
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String[] getAll()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String formatValue(String value)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int compare(StringPair one, StringPair two)
		{
			throw new UnsupportedOperationException();
		}
	};
	
	String prettyName;

	Sortables(String prettyName)
	{
		this.prettyName = prettyName;
	}

	public String toString()
	{
		return prettyName;
	}

	public abstract String getFrom(BugInstance bug);
	public String[] getAll()
	{
		return getAll(BugSet.getMainBugSet());
	}
	public String[] getAll(BugSet set)
	{
		return set.getAll(this);
	}
	
	public String formatValue(String value)
	{
		return value;
	}
	
	public int compare(StringPair one, StringPair two)
	{
		// Lexicographical by default
		return one.value.compareTo(two.value);
	}
	
	public String[] getAllSorted()
	{
		return getAllSorted(BugSet.getMainBugSet());
	}
	
	public String[] getAllSorted(BugSet set)
	{
		String[] values = getAll(set);
		StringPair[] pairs = new StringPair[values.length];
		for (int i = 0; i < values.length; i++)
			pairs[i] = new StringPair(this, values[i]);
		Arrays.sort(pairs, this);
		for (int i = 0; i < values.length; i++)
			values[i] = pairs[i].value;
		return values;
	}
	
	public Comparator<BugLeafNode> getBugLeafNodeComparator()
	{
		return new Comparator<BugLeafNode>()
		{
			public int compare(BugLeafNode one, BugLeafNode two)
			{
				Sortables key = Sortables.this;
				return Sortables.this.compare(new StringPair(key, key.getFrom(one.getBug())), new StringPair(key, key.getFrom(two.getBug())));
			}
		};
	}
	
	public static Sortables getSortableByPrettyName(String name)
	{
		for (Sortables s: Sortables.values())
		{
			if (s.prettyName.equals(name))
				return s;
		}
			return null;
	}
}
