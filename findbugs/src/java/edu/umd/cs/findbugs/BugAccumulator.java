/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Location;

/**
 * Accumulate warnings that may occur at multiple source locations,
 * consolidating them into a single warning.
 *
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class BugAccumulator {

    private final BugReporter reporter;

    private final boolean performAccumulation;

    private final Map<BugInstance, Data> map = new HashMap<BugInstance, Data>();

    private final HashMap<String, BugInstance> hashes = new HashMap<String, BugInstance>();

    private BugInstance lastBug;
    private SourceLineAnnotation lastSourceLine;

    static class Data {

        public Data(int priority, SourceLineAnnotation primarySource) {
            this.priority = priority;
            this.primarySource = primarySource;
        }

        int priority;

        SourceLineAnnotation primarySource;

        LinkedHashSet<SourceLineAnnotation> allSource = new LinkedHashSet<SourceLineAnnotation>();
    }

    /**
     * Constructor.
     *
     * @param reporter
     *            the BugReporter to which warnings should eventually be
     *            reported
     */
    public BugAccumulator(BugReporter reporter) {
        this.reporter = reporter;
        performAccumulation = AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.MERGE_SIMILAR_WARNINGS);
    }

    public @CheckForNull
    SourceLineAnnotation getLastBugLocation() {
        return lastSourceLine;
    }

    public void forgetLastBug() {
        Data d = map.get(lastBug);
        if (d != null) {
            d.allSource.remove(lastSourceLine);
            if (d.allSource.isEmpty()) {
                map.remove(lastBug);
                hashes.remove(lastBug.getInstanceHash());
            }
        }
        lastBug = null;
        lastSourceLine = null;
    }

    /**
     * Accumulate a warning at given source location.
     *
     * @param bug
     *            the warning
     * @param sourceLine
     *            the source location
     */
    public void accumulateBug(BugInstance bug, SourceLineAnnotation sourceLine) {
        if (sourceLine == null) {
            throw new NullPointerException("Missing source line");
        }
        int priority = bug.getPriority();
        if (!performAccumulation) {
            bug.addSourceLine(sourceLine);
        } else {
            bug.setPriority(Priorities.NORMAL_PRIORITY);
        }

        lastBug = bug;
        lastSourceLine = sourceLine;
        Data d = map.get(bug);
        if (d == null) {
            String hash = bug.getInstanceHash();
            BugInstance conflictingBug = hashes.get(hash);
            if (conflictingBug != null) {
                if (conflictingBug.getPriority() <= priority) {
                    return;
                }
                map.remove(conflictingBug);
            }
            d = new Data(priority, sourceLine);
            map.put(bug, d);
            hashes.put(hash, bug);
        } else if (d.priority > priority) {
            if (d.priority >= Priorities.LOW_PRIORITY) {
                reportBug(bug, d);
                d.allSource.clear();
            }
            d.priority = priority;
            d.primarySource = sourceLine;
        } else if (priority >= Priorities.LOW_PRIORITY && priority > d.priority) {
            bug.setPriority(priority);
            reporter.reportBug(bug);
            return;
        }
        d.allSource.add(sourceLine);
    }

    /**
     * Accumulate a warning at source location currently being visited by given
     * BytecodeScanningDetector.
     *
     * @param bug
     *            the warning
     * @param visitor
     *            the BytecodeScanningDetector
     */
    public void accumulateBug(BugInstance bug, BytecodeScanningDetector visitor) {
        SourceLineAnnotation source = SourceLineAnnotation.fromVisitedInstruction(visitor);
        accumulateBug(bug, source);
    }

    public Iterable<? extends BugInstance> uniqueBugs() {
        return map.keySet();

    }

    public Iterable<? extends SourceLineAnnotation> locations(BugInstance bug) {
        return map.get(bug).allSource;
    }

    /**
     * Report accumulated warnings to the BugReporter. Clears all accumulated
     * warnings as a side-effect.
     */
    public void reportAccumulatedBugs() {
        for (Map.Entry<BugInstance, Data> e : map.entrySet()) {
            BugInstance bug = e.getKey();
            Data d = e.getValue();
            reportBug(bug, d);
        }
        clearBugs();
    }

    public void reportBug(BugInstance bug, Data d) {
        bug.setPriority(d.priority);
        bug.addSourceLine(d.primarySource);
        HashSet<Integer> lines = new HashSet<Integer>();
        lines.add(d.primarySource.getStartLine());
        d.allSource.remove(d.primarySource);
        for (SourceLineAnnotation source : d.allSource) {
            if (lines.add(source.getStartLine())) {
                bug.addSourceLine(source);
                bug.describe(SourceLineAnnotation.ROLE_ANOTHER_INSTANCE);
            } /* else if (false && SystemProperties.ASSERTIONS_ENABLED) {
                AnalysisContext.logError("Skipping duplicated source warning for " + bug.getInstanceHash() + " " + bug.getMessage());
            }*/
        }
        reporter.reportBug(bug);
    }

    /**
     * Clear all accumulated bugs without reporting them
     */
    public void clearBugs() {
        map.clear();
        hashes.clear();
        lastBug = null;
        lastSourceLine = null;
    }

    public void accumulateBug(BugInstance bug, ClassContext classContext, Method method, Location location) {
        accumulateBug(bug, SourceLineAnnotation.fromVisitedInstruction(classContext, method, location));

    }

    public void accumulateBug(BugInstance bug, ClassContext classContext, MethodGen methodGen, String sourceFile,
            Location location) {
        accumulateBug(bug, SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, location.getHandle()));

    }
}
