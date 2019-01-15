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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Why this wasn't just called Filter is still somewhat of a mystery.
 * FilterMatchers are Filters, pass in a StringPair like Priority, High and all
 * the high priority bugs disappear, Its that easy.
 */
@Deprecated
public class FilterMatcher implements Matcher, Serializable, Comparable<FilterMatcher> {
    enum FilterWhere {
        FILTER_EXACTLY, FILTER_AT_OR_AFTER, FILTER_AT_OR_BEFORE, FILTER_ALL_BUT
    }

    private static final long serialVersionUID = -4859486064351510016L;

    private final Sortables filterBy;

    private final String value;

    private final FilterWhere mode;

    protected boolean active;

    public FilterMatcher(SortableValue sp) {
        this(sp.key, sp.value);
    }

    Sortables getFilterBy() {
        return filterBy;
    }

    String getValue() {
        return value;
    }

    public FilterMatcher(Sortables filterBy, String value, FilterWhere mode) // 0
    // =
    // exactly;
    // 1
    // =
    // at
    // or
    // after;
    // 2
    // =
    // at
    // or
    // before;
    // 3
    // =
    // not
    // at
    {
        this.filterBy = filterBy;
        this.value = value;
        this.mode = mode;
        this.active = true;
    }

    public FilterMatcher(Sortables filterBy, String value) {
        this.filterBy = filterBy;
        this.value = value;
        this.mode = FilterWhere.FILTER_EXACTLY;
        this.active = true;
    }

    public void setActive(boolean active) {
        if (active != this.active) {
            this.active = active;
            if (active == true) {
                FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
            } else {
                FilterActivity.notifyListeners(FilterListener.Action.UNFILTERING, null);
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        if (!active) {
            return true;
        }

        SortableStringComparator ssc = filterBy.getComparator();
        int compare = ssc.compare(filterBy.getFrom(bugInstance), value);
        switch (mode) {
        case FILTER_EXACTLY:
            return (compare != 0);
        case FILTER_AT_OR_AFTER:
            return (compare < 0);
        case FILTER_AT_OR_BEFORE:
            return (compare > 0);
        case FILTER_ALL_BUT:
            return (compare == 0);
        default:
            return true;
        }
    }

    @Override
    public String toString() {
        switch (mode) {
        case FILTER_EXACTLY:
            return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " "
            + edu.umd.cs.findbugs.L10N.getLocalString("mode.equal_to", "equal to") + " " + filterBy.formatValue(value);
        case FILTER_AT_OR_AFTER:
            return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " "
            + edu.umd.cs.findbugs.L10N.getLocalString("mode.at_or_after", "at or after") + " "
            + filterBy.formatValue(value);
        case FILTER_AT_OR_BEFORE:
            return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " "
            + edu.umd.cs.findbugs.L10N.getLocalString("mode.at_or_before", "at or before") + " "
            + filterBy.formatValue(value);
        case FILTER_ALL_BUT:
            return filterBy.toString() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " "
            + edu.umd.cs.findbugs.L10N.getLocalString("mode.not_equal_to", "not equal to") + " "
            + filterBy.formatValue(value);
        default:
            throw new RuntimeException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }

        if (filterBy.equals(((FilterMatcher) o).filterBy) && value.equals(((FilterMatcher) o).value)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode() + filterBy.hashCode();
    }

    @Override
    public int compareTo(FilterMatcher that) {
        if (this.filterBy != that.filterBy) {
            return (this.filterBy.ordinal() < that.filterBy.ordinal() ? -1 : 1);
        }

        return this.value.compareTo(that.value);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        throw new UnsupportedOperationException();
    }
}
