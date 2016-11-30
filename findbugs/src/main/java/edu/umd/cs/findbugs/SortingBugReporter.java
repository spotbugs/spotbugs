/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2006 University of Maryland
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

import java.util.Iterator;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * A BugReporter which stores all of the reported bug instances, and sorts them
 * by class name before printing them.
 */
public class SortingBugReporter extends TextUIBugReporter {
    private final SortedBugCollection bugCollection = new SortedBugCollection();

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
        // Don't need to do anything special, since we won't be
        // reporting statistics.
    }

    @Override
    public void doReportBug(BugInstance bugInstance) {
        if (bugCollection.add(bugInstance)) {
            notifyObservers(bugInstance);
        }
    }

    @Override
    public void finish() {
        Iterator<BugInstance> i = bugCollection.iterator();
        while (i.hasNext()) {
            BugInstance bugInstance = i.next();
            printBug(bugInstance);
        }

        outputStream.close();
    }

    @Override
    public @Nonnull
    BugCollection getBugCollection() {
        return bugCollection;
    }
}

