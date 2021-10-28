package edu.umd.cs.findbugs;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class BugReporterDispatcherTest {
    @Test(expected = IllegalArgumentException.class)
    public void createWithNoReportToDelegate() {
        new BugReportDispatcher(new ArrayList<>());
    }

    @Test
    public void dispatchingMethod() {
        SortingBugReporter bugReporter = new SortingBugReporter();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        bugReporter.setOutputStream(new PrintStream(outStream));
        BugReportDispatcher dispatcher = new BugReportDispatcher(Arrays.asList(bugReporter));
        dispatcher.logError("message");

        assertThat(outStream.toString(), is(""));
    }

    @Test
    public void exceptionsAreSuppressed() {
        BugReportDispatcher dispatcher = new BugReportDispatcher(Arrays.asList(new BrokenBugReporter(), new BrokenBugReporter()));
        try {
            dispatcher.logError("try to throw an error");
            fail();
        } catch (BrokenBugReporterException e) {
            assertThat(e.getSuppressed().length, is(1));
            assertThat(e.getSuppressed()[0], is(instanceOf(BrokenBugReporterException.class)));
        }
    }

    static final class BrokenBugReporterException extends RuntimeException {
    }
    static final class BrokenBugReporter extends PrintingBugReporter {
        @Override
        public void logError(String message) {
            throw new BrokenBugReporterException();
        }
    }
}
