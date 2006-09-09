package edu.umd.cs.findbugs.gui2;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;

/**
 * BugSet is what we use instead of SortedBugCollections.  BugSet is somewhat poorly named, in that its actually a HashList of bugs, 
 * not a Set of them.  (It can't be a set because we need to be able to sort it, also, HashList is great for doing contains and indexOf, its just slow for removing which we never need to do)  The power of BugSet is in query.  You can query a BugSet with a BugAspects, a list of StringPairs like <priority,high>, <designation,unclassified>
 * and you will get out a new BugSet containing all of the bugs that are both high priority and unclassified.  Also, after the first time a query is made, the results will come back instantly on future calls
 * because the old queries are cached.  Note that this caching can also lead to issues, problems with the BugTreeModel and the JTree getting out of sync, if there comes a time when the model and tree are out of sync
 * but come back into sync if the tree is rebuilt, say by sorting the column headers, it probably means that resetData needs to be called on the model after doing one of its operations.  
 * 
 * @author Dan
 *
 */
public class BugSet implements Iterable<BugLeafNode>{
	
	private HashList<BugLeafNode> mainList;
	private HashMap<StringPair,BugSet> doneMap;
	private HashMap<StringPair,Boolean>   doneContainsMap;
	private HashMap<Sortables,HashList<String>> sortablesToStrings;
	

	private static BugSet mainBugSet=null;

//	private ThreadMXBean bean = ManagementFactory.getThreadMXBean();
	/** mainBugSet should probably always be the same as the data field in the current BugTreeModel
	 * we haven't run into any issues where it isn't, but if the two aren't equal using ==, problems might occur.
	 * If these problems do occur, See BugTreeModel.resetData() and perhaps adding a setAsRootAndCache() to it would fix the issue.
	 * This is not done right now for fear it might be slow.
	 */
	public static BugSet getMainBugSet()
	{
		return mainBugSet;
	}
	
	/**
	 * Gets all the string values out of the bugs in the set
	 * @param s The Sortables you want all values for
	 * @return all values of the sortable passed in that occur in this bugset
	 */
	public String[] getAll(Sortables s)
	{
		HashList<String> list=sortablesToStrings.get(s);
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Creates a filterable dataset from the set passed in.  The first time this is used is from outside to create the main data list
	 * After that BugSet will create new smaller filtered sets and store them using this method.
	 * @param filteredSet
	 */
	BugSet(ArrayList<BugLeafNode> filteredSet)
	{
		this.mainList=new HashList<BugLeafNode>((ArrayList<BugLeafNode>)filteredSet.clone());
		doneMap=new HashMap<StringPair,BugSet>();
		doneContainsMap=new HashMap<StringPair,Boolean>();
		cacheSortables();
	}
	
	/**
	 * Sets the BugSet passed in to be the mainBugSet, this should always match up with the data set in the BugTreeModel
	 * @param bs
	 */
	static void setAsRootAndCache(BugSet bs)
	{
		mainBugSet=bs;
		bs.sortList();
		bs.cacheSortables();
	}
	
	/**
	 * we cache all values of each sortable that appear in the BugSet as we create it using cacheSortables, this makes it
	 * possible to only show branches that actually have bugs in them, and makes it faster by caching the results.
	 */
	void cacheSortables()
	{
		sortablesToStrings=new HashMap<Sortables,HashList<String>>();
		for (Sortables key: Sortables.values())
			if (key != Sortables.DIVIDER)
			{
				HashList<String> list=new HashList<String>();
				sortablesToStrings.put(key,list);
			}
		
		ArrayList<BugLeafNode> bugNodes=new ArrayList<BugLeafNode>();
		for(BugLeafNode p:mainList)
		{
			if (ProjectSettings.getInstance().getAllMatchers().match(p.getBug()))
				bugNodes.add(p);
		}
		
		for (BugLeafNode b:bugNodes)
		{
			BugInstance bug=b.getBug();
			BugPattern bugP=bug.getBugPattern();
			
			if (bugP==null)
			{
				assert false;
				if (MainFrame.DEBUG) System.err.println("A bug pattern was not found for "+bug.getMessage());
				continue;
			}

			for (Sortables key: Sortables.values())
				if (key != Sortables.DIVIDER)
				{
					HashList<String> list=sortablesToStrings.get(key);
					
					String value=key.getFrom(bug);
					if (!list.contains(value))
						list.add(value);
					sortablesToStrings.put(key,list);
				}
		}	
		
		for (Sortables key: Sortables.values())
			if (key != Sortables.DIVIDER)
				Collections.sort(sortablesToStrings.get(key));
	}
	
	/** used to update the status bar in mainframe with the number of bugs that are filtered out */ 
	static int countFilteredBugs()
	{
		CompoundMatcher cm=ProjectSettings.getInstance().getAllMatchers();
		int result = 0;
		for (BugLeafNode bug : mainBugSet.mainList)
			if (!cm.match(bug.getBug()))
				result++;

		return result;
	}
		
	/**
	 * Copy constructor, also used to make sure things are recalculated
	 * @param copySet
	 */
	//Note: THIS CLEARS THE CACHES OF DONE SETS!
	BugSet(BugSet copySet)
	{
		this.mainList=copySet.mainList;
		doneMap=new HashMap<StringPair,BugSet>();
		doneContainsMap=new HashMap<StringPair,Boolean>();
		cacheSortables();
	}
	

	/**
	 * A String pair has a key and a value.  The key is the general category ie: Type
	 * The value is the value  ie: Malicious Code.  
	 * 
	 * Query looks through a BugLeafNode set with a keyValuePair to see which BugLeafNodes inside
	 * match the value under the category of key.  
	 * 
	 * passing in a key of Abbrev and a value of MS should return a new BugSet with all the Mutable Static bugs in the current set
	 * Note also:  This query will only be performed once, and then stored and reused if the same query is used again.  
	 * @param keyValuePair
	 * @return
	 */
	BugSet query(StringPair keyValuePair)
	{
		if (doneMap.containsKey(keyValuePair))
			return doneMap.get(keyValuePair);
		ArrayList<BugLeafNode> bugs=new ArrayList<BugLeafNode>();
		
		for(BugLeafNode b:mainList)
		{
			if (b.matches(keyValuePair))
				bugs.add(b);
		}
		
		BugSet temp=new BugSet(bugs);
		doneMap.put(keyValuePair,temp);
		return temp;
	}

	/* Sort the contents of the list by the Sortables in the order after the divider, if any. */
	void sortList()
	{
		// Go backward through the sort order, sorting the entire list: that achieves the correct order
			// But it takes waaaay too long.
		final List<Sortables> order = MainFrame.getInstance().getSorter().getOrderAfterDivider();
//		for (int i = order.size() - 1; i >= 0; i--)
//			Collections.sort(mainList, order.get(i).getBugLeafNodeComparator());
		
		Collections.sort(mainList, new Comparator<BugLeafNode>(){
			public int compare(BugLeafNode one, BugLeafNode two)
			{
				for (Sortables i : order)
				{
					int result = i.getBugLeafNodeComparator().compare(one, two);
					if (result != 0)
						return result;
				}
				// If still here, they're really equal
				return 0;
			}
		});
	}
	
	/**
	 * 
	 * Matches takes two strings, key, and value, and a BugLeafNode p.  If that BugLeafNode's value under the category of key 
	 * matches the value passed in, it returns true.  Otherwise, its false.  
	 * 
	 * 
	 * @param key
	 * @param value
	 * @param p
	 * @return
	 */	
	public boolean contains(StringPair keyValuePair)
	{
		if (doneContainsMap.containsKey(keyValuePair))
			return doneContainsMap.get(keyValuePair);
		
		for(BugLeafNode p:filterNoCache().mainList)
		{
			if (p.matches(keyValuePair))
			{
				doneContainsMap.put(keyValuePair,true);
				return true;
			}
		}
		doneContainsMap.put(keyValuePair,false);
		return false;
	}
	
	
	/**
	 *Gives you back the BugSet containing all bugs that match your query
	 */
	public BugSet query(BugAspects a)
	{
		BugSet result=this;
		for (StringPair sp:a)
		{
			result=result.query(sp);
		}

		return result;
	}
	
	public int sizeUnfiltered()
	{
		return mainList.size();
	}
/*	
	public Iterator<BugLeafNode> iterator() {
		return mainList.iterator();
	}
*/	
	public int indexOfUnfiltered(BugLeafNode p)
	{
		return mainList.indexOf(p);
	}
	
	public BugLeafNode getUnfiltered(int index)
	{
		return mainList.get(index);
	}
	
	public Iterator<BugLeafNode> iterator()
	{	
		return mainList.iterator();
	}
	
	////////Filtered API
	
	BugSet(ArrayList<BugLeafNode> filteredSet, boolean cacheSortables)
	{
		this.mainList=new HashList<BugLeafNode>((ArrayList<BugLeafNode>)filteredSet.clone());
		doneMap=new HashMap<StringPair,BugSet>();
		doneContainsMap=new HashMap<StringPair,Boolean>();
		if (cacheSortables)
			cacheSortables();
	}
	
	public BugSet filterNoCache()
	{
		
		Matcher m=ProjectSettings.getInstance().getAllMatchers();
		ArrayList<BugLeafNode> people=new ArrayList<BugLeafNode>();
		for(BugLeafNode p:mainList)
		{
			if (m.match(p.getBug()))
				people.add(p);
		}
		return new BugSet(people,false);
	}
	
	public int size()
	{
		return filterNoCache().sizeUnfiltered();
	}
	
	public int indexOf(BugLeafNode p)
	{
		return filterNoCache().indexOfUnfiltered(p);
	}
	
	public BugLeafNode get(int index)
	{
		return filterNoCache().getUnfiltered(index);
	}
}