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
import java.util.Iterator;

import edu.umd.cs.findbugs.filter.Matcher;

/**
 * These are the branches in our tree, each branch forms a complete query that
 * could be sent to the main bugset to return all the bugs it contains For
 * example, a single bugAspects could be {@literal <priority,high>} or it could be
 * {@literal <priority,high>}, {@literal <designation,must fix>},{@literal <class,fishpond>},
 * {@literal <package,default>}
 *
 * In this implementation, {@literal <priority,high>},{@literal <designation,unclassified>} is
 * different from {@literal <designation,unclassified>},{@literal <priority,high>}. (I'm not talking
 * about the fact we use the .equals from ArrayList, I'm talking about what a
 * query would return, though both are true) For a speed boost, this class could
 * be rewritten to make these equal, BugSet could be rewritten to cache full
 * queries off the main BugSet, (instead of caching each part of the query
 * separately in the BugSets created) and resetData could be rewritten to work
 * more like Swing's validate, only clearing data if the data is wrong. This
 * would save time after changing certain aspects of the tree. Just an idea, I
 * wouldn't suggest it unless its absolutely necessary. -Dan
 *
 *
 * @author All of us
 */
public class BugAspects implements Iterable<BugAspects.SortableValue> {
    private static final long serialVersionUID = -5503915081879996968L;

    private int count = -1;

    private ArrayList<BugAspects.SortableValue> lst = new ArrayList<>();

    public SortableValue last() {
        return lst.get(lst.size() - 1);
    }

    public int size() {
        return lst.size();
    }

    public SortableValue get(int i) {
        return lst.get(i);
    }

    @Override
    public String toString() {
        if (lst.isEmpty()) {
            return edu.umd.cs.findbugs.L10N.getLocalString("tree.bugs", "Bugs") + " (" + count + ")";
        } else {
            if (count == -1) {
                return last().value;
            } else {
                return last().key.formatValue(last().value) + " (" + count + ")";
            }
        }
    }

    /**
     * This is how the numbers after the branches contain the number of bugs in
     * them, even if they aren't the final branch
     *
     * @param count
     */
    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public BugAspects() {
        super();
    }

    public BugAspects(BugAspects a) {
        lst = new ArrayList<>(a.lst);
        count = a.count;
    }

    public void add(SortableValue sp) {
        lst.add(sp);
    }

    public BugAspects addToNew(SortableValue sp) {
        BugAspects result = new BugAspects(this);
        result.lst.add(sp);
        return result;
    }

    public Matcher getMatcher() {
        return FilterFactory.makeAndMatcher(lst);
    }

    public StackedFilterMatcher getStackedFilterMatcher() {
        FilterMatcher[] filters = new FilterMatcher[lst.size()];
        for (int i = 0; i < filters.length; i++) {
            filters[i] = new FilterMatcher(lst.get(i));
        }
        StackedFilterMatcher sfm = new StackedFilterMatcher(filters);
        return sfm;
    }

    public BugSet getMatchingBugs(BugSet theSet) {
        return theSet.getBugsMatchingFilter(this.getStackedFilterMatcher());
    }

    static class SortableValue {
        final public Sortables key;

        final public String value;

        public SortableValue(Sortables key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int hashCode() {
            return key.hashCode() + value.hashCode();
        }

        @Override
        public boolean equals(Object that) {
            if (!(that instanceof SortableValue)) {
                return false;
            }
            SortableValue thatStringPair = ((SortableValue) that);
            return this.key.equals(thatStringPair.key) && this.value.equals(thatStringPair.value);
        }

        @Override
        public String toString() {
            return key + ":" + value;
        }
    }

    @Override
    public Iterator<SortableValue> iterator() {
        return lst.iterator();
    }
}
