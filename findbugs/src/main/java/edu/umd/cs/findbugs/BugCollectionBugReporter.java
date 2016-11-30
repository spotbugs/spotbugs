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

import java.io.PrintWriter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class BugCollectionBugReporter extends TextUIBugReporter implements Debug {
    private final SortedBugCollection bugCollection;

    private final Project project;

    @CheckForNull private final PrintWriter writer;

    public BugCollectionBugReporter(Project project) {
        this(project, null);
    }

    public BugCollectionBugReporter(Project project, @CheckForNull PrintWriter writer) {
        this.project = project;
        this.bugCollection = new SortedBugCollection(getProjectStats(), project);
        bugCollection.setTimestamp(System.currentTimeMillis());
        this.writer = writer;
    }
    public Project getProject() {
        return project;
    }

    @Override
    public @Nonnull
    BugCollection getBugCollection() {
        return bugCollection;
    }

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
    }

    @Override
    public void logError(String message) {
        bugCollection.addError(message);
        super.logError(message);
    }

    @Override
    public void logError(String message, Throwable e) {
        if (e instanceof MissingClassException) {
            MissingClassException e2 = (MissingClassException) e;
            reportMissingClass(e2.getClassNotFoundException());
            return;
        }
        if (e instanceof MethodUnprofitableException) {
            // TODO: log this
            return;
        }
        bugCollection.addError(message, e);
        super.logError(message, e);
    }

    @Override
    public void reportMissingClass(ClassNotFoundException ex) {
        String missing = AbstractBugReporter.getMissingClassName(ex);
        if (!isValidMissingClassMessage(missing)) {
            return;
        }
        bugCollection.addMissingClass(missing);
        super.reportMissingClass(ex);
    }

    @Override
    public void doReportBug(BugInstance bugInstance) {
        if (VERIFY_INTEGRITY) {
            checkBugInstance(bugInstance);
        }
        if (bugCollection.add(bugInstance)) {
            notifyObservers(bugInstance);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
     */
    @Override
    public BugReporter getRealBugReporter() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    @Override
    public void finish() {
        bugCollection.bugsPopulated();
        if (writer != null) {
            writer.flush();
        }
    }

    /**
     * Emit one line of the error message report. By default, error messages are
     * printed to System.err. Subclasses may override.
     *
     * @param line
     *            one line of the error report
     */
    @Override
    protected void emitLine(String line) {
        if (writer == null) {
            super.emitLine(line);
            return;
        }
        line = line.replaceAll("\t", "  ");
        writer.println(line);
    }


}

