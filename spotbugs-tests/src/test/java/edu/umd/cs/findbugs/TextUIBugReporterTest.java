package edu.umd.cs.findbugs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1032">GitHub issue #1032</a>
 */
class TextUIBugReporterTest {

    private static class CapturingBugReporter extends PrintingBugReporter {
        final List<String> lines = new ArrayList<>();

        @Override
        protected void emitLine(String line) {
            lines.add(line.replace("\t", "  "));
        }
    }

    @Test
    void analysisErrorReportsCause() {
        CapturingBugReporter reporter = new CapturingBugReporter();
        IllegalStateException cause = new IllegalStateException("root cause");
        StackTraceElement causeFrame = new StackTraceElement("com.example.RootCause", "fail", "RootCause.java", 42);
        cause.setStackTrace(new StackTraceElement[] { causeFrame });
        RuntimeException wrapper = new RuntimeException("wrapper", cause);

        reporter.reportAnalysisError(new AnalysisError("Exception while analyzing Foo.bar:()V", wrapper));

        assertThat(reporter.lines, hasItem("    " + wrapper));
        int causedByIndex = reporter.lines.indexOf("    Caused by: " + cause);
        assertThat(causedByIndex, greaterThanOrEqualTo(0));
        assertThat(reporter.lines, hasSize(greaterThan(causedByIndex + 1)));
        assertThat(reporter.lines.get(causedByIndex + 1), is("      At " + causeFrame));
    }

    @Test
    void analysisErrorReportsCauseWithoutStackTrace() {
        CapturingBugReporter reporter = new CapturingBugReporter();
        reporter.setReportStackTrace(false);
        IllegalStateException cause = new IllegalStateException("root cause");

        reporter.reportAnalysisError(new AnalysisError("error", new RuntimeException("wrapper", cause)));

        assertThat(reporter.lines, hasItem("    Caused by: " + cause));
        assertThat(reporter.lines, not(hasItem(startsWith("      At "))));
    }

    @Test
    void analysisErrorWithoutCause() {
        CapturingBugReporter reporter = new CapturingBugReporter();

        reporter.reportAnalysisError(new AnalysisError("error", new RuntimeException("no cause")));

        assertThat(reporter.lines, not(hasItem(startsWith("    Caused by:"))));
    }
}
