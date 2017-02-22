/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.filter.Matcher;

public class FilterBugReporter extends DelegatingBugReporter {
    private static final boolean DEBUG = SystemProperties.getBoolean("filter.debug");

    private final Matcher filter;

    private final boolean include;

    public FilterBugReporter(BugReporter realBugReporter, Matcher filter, boolean include) {
        super(realBugReporter);
        this.filter = filter;
        this.include = include;
    }

    @Override
    public void reportBug(@Nonnull BugInstance bugInstance) {
        if (DEBUG) {
            System.out.print("Match ==> ");
        }
        boolean match = filter.match(bugInstance);
        if (DEBUG) {
            System.out.println(match ? "YES" : "NO");
        }
        if (include == match) {
            getDelegate().reportBug(bugInstance);
        }
    }
}

