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

/**
 * A useful enum for dealing with all the types of filterable and sortable data in BugInstances
 * This is the preferred way for getting the information out of a BugInstance and formatting it for display
 * It also has the comparators for the different types of data
 * 
 * @author Reuven
 */
public enum Sortables implements Comparator<StringPair>
{
	PRIORITY("Priority")
	{
		public String getFrom(BugInstance bug)
		{
			return String.valueOf(bug.getPriority());
		}
		
		@Override
		public String formatValue(String value)
		{
			if (value.equals(String.valueOf(Detector.HIGH_PRIORITY)))
				return "High";
			if (value.equals(String.valueOf(Detector.NORMAL_PRIORITY)))
				return "Normal";
			if (value.equals(String.valueOf(Detector.LOW_PRIORITY)))
				return "Low";
			if (value.equals(String.valueOf(Detector.EXP_PRIORITY)))
				return "Experimental";
			if (value.equals(String.valueOf(Detector.IGNORE_PRIORITY)))
				return "Ignore"; // This probably shouldn't ever happen, but what the hell, let's be complete
			
			throw new IllegalArgumentException(value + " is not a valid priority");
		}
		
		@Override
		public int compare(StringPair one, StringPair two)
		{
			// Numerical
			return Integer.valueOf(one.value).compareTo(Integer.valueOf(two.value));
		}
	},
	CLASS("Class")
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
	PACKAGE("Package")
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
	CATEGORY("Category")
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
	DESIGNATION("Designation")
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
			List<String> sortedDesignations=I18N.instance().getUserDesignationKeys();
			Collections.sort(sortedDesignations,I18N.designationKeyComparator);
			return sortedDesignations.toArray(new String[I18N.instance().getUserDesignationKeys().size()]);
		}
	},
	BUGCODE("Bug Kind")
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
	TYPE("Bug Pattern")
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
