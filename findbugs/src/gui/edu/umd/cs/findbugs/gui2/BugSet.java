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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;

/**
 * BugSet is what we use instead of SortedBugCollections. BugSet is somewhat
 * poorly named, in that its actually a HashList of bugs, not a Set of them. (It
 * can't be a set because we need to be able to sort it, also, HashList is great
 * for doing contains and indexOf, its just slow for removing which we never
 * need to do) The power of BugSet is in query. You can query a BugSet with a
 * BugAspects, a list of StringPairs like <priority,high>,
 * <designation,unclassified> and you will get out a new BugSet containing all
 * of the bugs that are both high priority and unclassified. Also, after the
 * first time a query is made, the results will come back instantly on future
 * calls because the old queries are cached. Note that this caching can also
 * lead to issues, problems with the BugTreeModel and the JTree getting out of
 * sync, if there comes a time when the model and tree are out of sync but come
 * back into sync if the tree is rebuilt, say by sorting the column headers, it
 * probably means that resetData needs to be called on the model after doing one
 * of its operations.
 *
 * @author Dan
 *
 */
public class BugSet implements Iterable<BugLeafNode> {

    private ArrayList<BugLeafNode> mainList;

    private final HashMap<SortableValue, BugSet> doneMap;

    private final HashMap<SortableValue, Boolean> doneContainsMap;

    private HashMap<Sortables, String[]> sortablesToStrings;

    private static BugSet mainBugSet = null;

    /**
     * mainBugSet should probably always be the same as the data field in the
     * current BugTreeModel we haven't run into any issues where it isn't, but
     * if the two aren't equal using ==, problems might occur. If these problems
     * do occur, See BugTreeModel.resetData() and perhaps adding a
     * setAsRootAndCache() to it would fix the issue. This is not done right now
     * for fear it might be slow.
     */
    public static BugSet getMainBugSet() {
        return mainBugSet;
    }

    /**
     * Gets all the string values out of the bugs in the set
     *
     * @param s
     *            The Sortables you want all values for
     * @return all values of the sortable passed in that occur in this bugset,
     *         in order based on the sortable's compare method.
     */
    public String[] getAll(Sortables s) {
        return getDistinctValues(s);
    }

    /**
     * Creates a filterable dataset from the set passed in. The first time this
     * is used is from outside to create the main data list After that BugSet
     * will create new smaller filtered sets and store them using this method.
     *
     * @param filteredSet
     */
    BugSet(Collection<? extends BugLeafNode> filteredSet) {
        this.mainList = new ArrayList<BugLeafNode>(filteredSet);
        doneMap = new HashMap<SortableValue, BugSet>();
        doneContainsMap = new HashMap<SortableValue, Boolean>();
        cacheSortables();
    }

    BugSet(BugCollection bugCollection) {
        this(Collections.<BugLeafNode> emptyList());
        for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
            mainList.add(new BugLeafNode(i.next()));
        }

    }

    /**
     * Sets the BugSet passed in to be the mainBugSet, this should always match
     * up with the data set in the BugTreeModel
     *
     * @param bs
     */
    static void setAsRootAndCache(BugSet bs) {
        mainBugSet = bs;
        bs.sortList();
        bs.cacheSortables();
    }

    static boolean suppress(BugLeafNode p) {
        return !MainFrame.getInstance().shouldDisplayIssue(p.getBug());
    }

    /**
     * we cache all values of each sortable that appear in the BugSet as we
     * create it using cacheSortables, this makes it possible to only show
     * branches that actually have bugs in them, and makes it faster by caching
     * the results.
     */
    void cacheSortables() {
        sortablesToStrings = new HashMap<Sortables, String[]>();
    }

    String[] getDistinctValues(Sortables key) {
        String[] list = sortablesToStrings.get(key);
        if (list == null) {
            list = computeDistinctValues(key);
            sortablesToStrings.put(key, list);
        }
        return list;
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    String[] computeDistinctValues(Sortables key) {

        if (key == Sortables.DIVIDER) {
            return EMPTY_STRING_ARRAY;
        }

        Collection<String> list = new HashSet<String>();

        for (BugLeafNode p : mainList) {
            if (suppress(p)) {
                continue;
            }
            BugInstance bug = p.getBug();

            String value = key.getFrom(bug);
            list.add(value);

        }
        String result[] = list.toArray(new String[list.size()]);
        Collections.sort(Arrays.asList(result), new SortableStringComparator(key));
        return result;

    }

    /**
     * used to update the status bar in mainframe with the number of bugs that
     * are filtered out
     */
    static int countFilteredBugs() {
        int result = 0;
        for (BugLeafNode bug : getMainBugSet().mainList) {
            if (suppress(bug)) {
                result++;
            }
        }

        return result;
    }

    /**
     * Copy constructor, also used to make sure things are recalculated
     *
     * @param copySet
     */
    // Note: THIS CLEARS THE CACHES OF DONE SETS!
    BugSet(BugSet copySet) {
        this.mainList = copySet.mainList;
        doneMap = new HashMap<SortableValue, BugSet>();
        doneContainsMap = new HashMap<SortableValue, Boolean>();
        cacheSortables();
    }

    /**
     * A String pair has a key and a value. The key is the general category ie:
     * Type The value is the value ie: Malicious Code.
     *
     * Query looks through a BugLeafNode set with a keyValuePair to see which
     * BugLeafNodes inside match the value under the category of key.
     *
     * passing in a key of Abbrev and a value of MS should return a new BugSet
     * with all the Mutable Static bugs in the current set Note also: This query
     * will only be performed once, and then stored and reused if the same query
     * is used again.
     */
    BugSet query(SortableValue keyValuePair) {
        if (doneMap.containsKey(keyValuePair)) {
            return doneMap.get(keyValuePair);
        }
        ArrayList<BugLeafNode> bugs = new ArrayList<BugLeafNode>();

        for (BugLeafNode b : mainList) {
            if (b.matches(keyValuePair)) {
                bugs.add(b);
            }
        }

        BugSet temp = new BugSet(bugs);
        doneMap.put(keyValuePair, temp);
        return temp;
    }

    /*
     * Sort the contents of the list by the Sortables in the order after the
     * divider, if any.
     */
    void sortList() {

        final List<Sortables> order = MainFrame.getInstance().getSorter().getOrderAfterDivider();

        Comparator<BugLeafNode> comparator = new Comparator<BugLeafNode>() {
            int compare(int one, int two) {
                if (one > two) {
                    return 1;
                } else if (one < two) {
                    return -1;
                }
                return 0;
            }

            @Override
            public int compare(BugLeafNode one, BugLeafNode two) {
                if (one == two) {
                    return 0;
                }
                int result;
                for (Sortables i : order) {
                    result = i.getBugLeafNodeComparator().compare(one, two);
                    if (result != 0) {
                        return result;
                    }
                }
                BugInstance bugOne = one.getBug();
                BugInstance bugTwo = two.getBug();
                result = bugOne.getPrimaryClass().getClassName().compareTo(bugTwo.getPrimaryClass().getClassName());
                if (result != 0) {
                    return result;
                }
                SourceLineAnnotation oneSource = bugOne.getPrimarySourceLineAnnotation();
                SourceLineAnnotation twoSource = bugTwo.getPrimarySourceLineAnnotation();
                result = oneSource.getClassName().compareTo(twoSource.getClassName());
                if (result != 0) {
                    return result;
                }
                result = compare(oneSource.getStartLine(), twoSource.getStartLine());
                if (result != 0) {
                    return result;
                }
                result = compare(oneSource.getEndLine(), twoSource.getEndLine());
                if (result != 0) {
                    return result;
                }
                result = compare(oneSource.getStartBytecode(), twoSource.getStartBytecode());
                if (result != 0) {
                    return result;
                }
                result = compare(oneSource.getEndBytecode(), twoSource.getEndBytecode());
                return result;

            }
        };
        ArrayList<BugLeafNode> copy = new ArrayList<BugLeafNode>(mainList);
        Collections.sort(copy, comparator);
        mainList = copy;

        if (SystemProperties.ASSERTIONS_ENABLED) {
            for(int i = 0; i < mainList.size(); i++) {
                BugLeafNode nodeI = mainList.get(i);

                for(int j = i+1; j < mainList.size(); j++) {
                    BugLeafNode nodeJ = mainList.get(j);
                    if (comparator.compare(nodeI, nodeJ) > 0) {
                        throw new AssertionError(
                                String.format("bug list isn't consistently sorted (%d:%s) vs. (%d:%s)",
                                        i, nodeI.getBug().getInstanceHash(), j, nodeJ.getBug().getInstanceHash()));
                    }
                }}
        }



    }

    /**
     *
     * Contains takes a key/value pair
     *
     * @param keyValuePair
     * @return true if a bug leaf from filterNoCache() matches the pair
     */
    public boolean contains(SortableValue keyValuePair) {
        if (doneContainsMap.containsKey(keyValuePair)) {
            return doneContainsMap.get(keyValuePair);
        }

        for (BugLeafNode p : filteredBugsCached().mainList) {
            if (p.matches(keyValuePair)) {
                doneContainsMap.put(keyValuePair, true);
                return true;
            }
        }
        doneContainsMap.put(keyValuePair, false);
        return false;
    }

    /**
     * Gives you back the BugSet containing all bugs that match your query
     */
    public BugSet query(BugAspects a) {
        BugSet result = this;
        for (SortableValue sp : a) {
            result = result.query(sp);
        }

        return result;
    }

    public int sizeUnfiltered() {
        return mainList.size();
    }

    public int indexOfUnfiltered(BugLeafNode p) {
        return mainList.indexOf(p);
    }

    public BugLeafNode getUnfiltered(int index) {
        return mainList.get(index);
    }

    @Override
    public Iterator<BugLeafNode> iterator() {
        return mainList.iterator();
    }

    // //////Filtered API

    BugSet(ArrayList<BugLeafNode> filteredSet, boolean cacheSortables) {
        this.mainList = new ArrayList<BugLeafNode>(filteredSet);
        doneMap = new HashMap<SortableValue, BugSet>();
        doneContainsMap = new HashMap<SortableValue, Boolean>();
        if (cacheSortables) {
            cacheSortables();
        }
    }

    private BugSet filteredBugsNoCache() {

        ArrayList<BugLeafNode> people = new ArrayList<BugLeafNode>();
        for (BugLeafNode p : mainList) {
            if (!suppress(p)) {
                people.add(p);
            }
        }
        return new BugSet(people, false);
    }

    BugSet cache = null;

    public void clearCache() {
        cache = null;
    }

    private BugSet filteredBugsCached() {
        if (cache == null) {
            cache = filteredBugsNoCache();
        }
        return cache;
    }

    public BugSet getBugsMatchingFilter(Matcher m) {
        ArrayList<BugLeafNode> people = new ArrayList<BugLeafNode>();
        for (BugLeafNode p : mainList) {
            if (!(m.match(p.getBug()))) {
                people.add(p);
            }
        }
        return new BugSet(people, false);
    }

    public int size() {
        return filteredBugsCached().sizeUnfiltered();
    }

    public int indexOf(BugLeafNode p) {
        return filteredBugsCached().indexOfUnfiltered(p);
    }

    public BugLeafNode get(int index) {
        return filteredBugsCached().getUnfiltered(index);
    }
}
