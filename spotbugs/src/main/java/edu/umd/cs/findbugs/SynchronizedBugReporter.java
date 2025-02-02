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

import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * An implementation of {@link BugReporter} that synchronize all method invocations.
 *
 * @since 4.0
 */
class SynchronizedBugReporter implements BugReporter {
    @NonNull
    private final BugReporter delegate;

    SynchronizedBugReporter(@NonNull BugReporter delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public synchronized void setErrorVerbosity(int level) {
        delegate.setErrorVerbosity(level);
    }

    @Override
    public synchronized void setPriorityThreshold(int threshold) {
        delegate.setPriorityThreshold(threshold);
    }

    @Override
    public synchronized void observeClass(ClassDescriptor classDescriptor) {
        delegate.observeClass(classDescriptor);
    }

    @Override
    public synchronized void reportBug(@Nonnull BugInstance bugInstance) {
        delegate.reportBug(bugInstance);
    }

    @Override
    public synchronized void logError(String message) {
        delegate.logError(message);
    }

    @Override
    public synchronized void reportMissingClass(ClassNotFoundException ex) {
        delegate.reportMissingClass(ex);
    }

    @Override
    public synchronized void reportMissingClass(ClassDescriptor classDescriptor) {
        delegate.reportMissingClass(classDescriptor);
    }

    @Override
    public synchronized void finish() {
        delegate.finish();
    }

    @Override
    public synchronized void reportQueuedErrors() {
        delegate.reportQueuedErrors();
    }

    @Override
    public synchronized void addObserver(BugReporterObserver observer) {
        delegate.addObserver(observer);
    }

    @Override
    public synchronized ProjectStats getProjectStats() {
        return delegate.getProjectStats();
    }

    @Override
    public synchronized void logError(String message, Throwable e) {
        delegate.logError(message, e);
    }

    /**
     * Report that we skipped some analysis of a method
     *
     * @param method
     */
    @Override
    public synchronized void reportSkippedAnalysis(MethodDescriptor method) {
        delegate.reportSkippedAnalysis(method);
    }

    @Override
    @CheckForNull
    public synchronized BugCollection getBugCollection() {
        return delegate.getBugCollection();
    }
}
