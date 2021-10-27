package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class BugReportDispatcher extends TextUIBugReporter {
    @NonNull
    private final List<TextUIBugReporter> reporters;

    BugReportDispatcher(Collection<TextUIBugReporter> reporters) {
        if (reporters == null || reporters.isEmpty()) {
            throw new IllegalArgumentException("No reporter provided");
        }
        this.reporters = new ArrayList<>(reporters);
    }

    @Override
    public void setErrorVerbosity(int level) {
        reporters.forEach(reporter -> reporter.setErrorVerbosity(level));
    }

    @Override
    public void setPriorityThreshold(int threshold) {
        reporters.forEach(reporter -> reporter.setPriorityThreshold(threshold));
    }

    @Override
    public void finish() {
        reporters.forEach(BugReporter::finish);
    }

    @Override
    public void reportQueuedErrors() {
        reporters.forEach(BugReporter::reportQueuedErrors);
    }

    @Override
    public void addObserver(BugReporterObserver observer) {
        reporters.forEach(reporter -> reporter.addObserver(observer));
    }

    @Override
    public ProjectStats getProjectStats() {
        return reporters.get(0).getProjectStats();
    }

    @Override
    protected void doReportBug(BugInstance bugInstance) {
        reporters.forEach(reporter -> reporter.doReportBug(bugInstance));
    }

    @CheckForNull
    @Override
    public BugCollection getBugCollection() {
        return reporters.get(0).getBugCollection();
    }

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
        reporters.forEach(reporter -> reporter.observeClass(classDescriptor));
    }

    @Override
    public void reportMissingClass(ClassNotFoundException ex) {
        reporters.forEach(reporter -> reporter.reportMissingClass(ex));
    }

    @Override
    public void reportMissingClass(ClassDescriptor classDescriptor) {
        reporters.forEach(reporter -> reporter.reportMissingClass(classDescriptor));
    }

    @Override
    public void logError(String message) {
        reporters.forEach(reporter -> reporter.logError(message));
    }

    @Override
    public void logError(String message, Throwable e) {
        reporters.forEach(reporter -> reporter.logError(message, e));
    }

    @Override
    public void reportSkippedAnalysis(MethodDescriptor method) {
        reporters.forEach(reporter -> reporter.reportSkippedAnalysis(method));
    }
}
