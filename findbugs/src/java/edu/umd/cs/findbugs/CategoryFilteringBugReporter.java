/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2006, University of Maryland
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

import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Filter reported warnings by category.
 */
public class CategoryFilteringBugReporter extends DelegatingBugReporter {
    private static final boolean DEBUG = SystemProperties.getBoolean("cfbreporter.debug");

    private final Set<String> categorySet;

    public CategoryFilteringBugReporter(BugReporter realBugReporter, Set<String> categorySet) {
        super(realBugReporter);
        this.categorySet = categorySet;
    }

    @Override
    public void reportBug(@Nonnull BugInstance bugInstance) {
        BugPattern bugPattern = bugInstance.getBugPattern();
        String category = bugPattern.getCategory();
        if (categorySet.contains(category)) {
            getDelegate().reportBug(bugInstance);
        } else {
            if (DEBUG) {
                System.out.println("CategoryFilteringBugReporter: filtered due to category " + category);
            }
        }
    }
}
