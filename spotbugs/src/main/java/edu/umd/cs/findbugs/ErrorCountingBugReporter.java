/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2007, University of Maryland
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

import java.util.HashSet;
import java.util.Set;

/**
 * A delegating bug reporter which counts reported bug instances, missing
 * classes, and serious analysis errors.
 */
public class ErrorCountingBugReporter extends DelegatingBugReporter {
    private int bugCount;

    private final HashSet<String> errors = new HashSet<String>();

    private final Set<String> missingClassSet = new HashSet<String>();

    public ErrorCountingBugReporter(BugReporter realBugReporter) {
        super(realBugReporter);
        this.bugCount = 0;

        // Add an observer to record when bugs make it through
        // all priority and filter criteria, so our bug count is
        // accurate.
        realBugReporter.addObserver(new BugReporterObserver() {
            @Override
            public void reportBug(BugInstance bugInstance) {
                ++bugCount;
            }
        });
    }

    public int getBugCount() {
        return bugCount;
    }

    public int getMissingClassCount() {
        return missingClassSet.size();
    }

    public int getErrorCount() {
        return errors.size();
    }

    @Override
    public void logError(String message) {
        if (errors.add(message)) {
            super.logError(message);
        }
    }

    @Override
    public void reportMissingClass(ClassNotFoundException ex) {
        String missing = AbstractBugReporter.getMissingClassName(ex);
        if (missing == null || missing.startsWith("[") || "java.lang.Synthetic".equals(missing)) {
            return;
        }
        if (missingClassSet.add(missing)) {
            super.reportMissingClass(ex);
        }
    }
}
