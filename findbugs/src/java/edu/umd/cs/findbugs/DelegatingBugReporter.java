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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * A BugReporter which delegates all method calls to another BugReporter. This
 * is useful for customizing the behavior of another bug reporter.
 *
 * @author David Hovemeyer
 */
public class DelegatingBugReporter implements BugReporter {
    private BugReporter delegate;

    /**
     * Constructor.
     *
     * @param delegate
     *            another BugReporter to delegate all BugReporter methods to
     */
    public DelegatingBugReporter(BugReporter delegate) {
        this.delegate = delegate;
    }

    protected BugReporter getDelegate() {
        return this.delegate;
    }

    public void setErrorVerbosity(int level) {
        delegate.setErrorVerbosity(level);
    }

    public void setPriorityThreshold(int threshold) {
        delegate.setPriorityThreshold(threshold);
    }

    public void observeClass(ClassDescriptor classDescriptor) {
        delegate.observeClass(classDescriptor);
    }

    public void reportBug(@Nonnull BugInstance bugInstance) {
        delegate.reportBug(bugInstance);
    }

    public void logError(String message) {
        delegate.logError(message);
    }

    public void reportMissingClass(ClassNotFoundException ex) {
        delegate.reportMissingClass(ex);
    }

    public void reportMissingClass(ClassDescriptor classDescriptor) {
        delegate.reportMissingClass(classDescriptor);
    }

    public void finish() {
        delegate.finish();
    }

    public void reportQueuedErrors() {
        delegate.reportQueuedErrors();
    }

    public void addObserver(BugReporterObserver observer) {
        delegate.addObserver(observer);
    }

    public ProjectStats getProjectStats() {
        return delegate.getProjectStats();
    }

    public void logError(String message, Throwable e) {
        if (e instanceof MethodUnprofitableException)
            return;
        delegate.logError(message, e);
    }

    /**
     * Report that we skipped some analysis of a method
     *
     * @param method
     */
    public void reportSkippedAnalysis(MethodDescriptor method) {
        delegate.reportSkippedAnalysis(method);
    }

    public @CheckForNull
    BugCollection getBugCollection() {
        return delegate.getBugCollection();
    }
}

// vim:ts=4
