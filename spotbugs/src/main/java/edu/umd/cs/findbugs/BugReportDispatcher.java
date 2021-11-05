package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

import javax.annotation.CheckForNull;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Bug reporter delegate actual operation to each bug reporter in the list.
 * It is designed to output multiple reports in batch.
 */
public class BugReportDispatcher implements ConfigurableBugReporter {
    @NonNull
    private final List<TextUIBugReporter> reporters;

    public BugReportDispatcher(Collection<TextUIBugReporter> reporters) {
        if (reporters == null || reporters.isEmpty()) {
            throw new IllegalArgumentException("No reporter provided");
        }
        this.reporters = new ArrayList<>(reporters);
    }

    @Override
    public void setErrorVerbosity(int level) {
        forEach(reporter -> reporter.setErrorVerbosity(level));
    }

    @Override
    public void setPriorityThreshold(int threshold) {
        forEach(reporter -> reporter.setPriorityThreshold(threshold));
    }

    @Override
    public void finish() {
        forEach(BugReporter::finish);
    }

    @Override
    public void reportQueuedErrors() {
        forEach(BugReporter::reportQueuedErrors);
    }

    @Override
    public void addObserver(BugReporterObserver observer) {
        forEach(reporter -> reporter.addObserver(observer));
    }

    @Override
    public ProjectStats getProjectStats() {
        return reporters.get(0).getProjectStats();
    }

    @Override
    public void reportBug(@NonNull BugInstance bugInstance) {
        forEach(reporter -> reporter.reportBug(bugInstance));
    }

    @CheckForNull
    @Override
    public BugCollection getBugCollection() {
        return reporters.get(0).getBugCollection();
    }

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
        forEach(reporter -> reporter.observeClass(classDescriptor));
    }

    @Override
    public void reportMissingClass(ClassNotFoundException ex) {
        forEach(reporter -> reporter.reportMissingClass(ex));
    }

    @Override
    public void reportMissingClass(ClassDescriptor classDescriptor) {
        forEach(reporter -> reporter.reportMissingClass(classDescriptor));
    }

    @Override
    public void logError(String message) {
        forEach(reporter -> reporter.logError(message));
    }

    @Override
    public void logError(String message, Throwable e) {
        forEach(reporter -> reporter.logError(message, e));
    }

    @Override
    public void reportSkippedAnalysis(MethodDescriptor method) {
        forEach(reporter -> reporter.reportSkippedAnalysis(method));
    }

    @Override
    public void setRankThreshold(int threshold) {
        forEach(reporter -> reporter.setRankThreshold(threshold));
    }

    @Override
    public void setUseLongBugCodes(boolean useLongBugCodes) {
        forEach(reporter -> reporter.setUseLongBugCodes(useLongBugCodes));
    }

    @Override
    public void setOutputStream(PrintStream outputStream) {
        forEach(reporter -> reporter.setOutputStream(outputStream));
    }

    /**
     * Consume each reporter one by one, and throw an exception if some of them.
     * @param consumer Operation to handle each reporter.
     */
    private void forEach(Consumer<TextUIBugReporter> consumer) {
        List<RuntimeException> exceptions = reporters.stream().map(reporter -> {
            try {
                consumer.accept(reporter);
                return null;
            } catch (RuntimeException thrown) {
                return thrown;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (exceptions.isEmpty()) {
            return;
        }

        RuntimeException head = exceptions.get(0);
        for (int i = 1; i < exceptions.size(); ++i) {
            head.addSuppressed(exceptions.get(i));
        }

        throw head;
    }
}
