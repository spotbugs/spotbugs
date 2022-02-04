package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
        bugReporter.setOutputStream(new PrintStream(outStream, false, StandardCharsets.UTF_8));

        BugReportDispatcher dispatcher = new BugReportDispatcher(Arrays.asList(bugReporter));
        dispatcher.setErrorVerbosity(BugReporter.NORMAL);
        dispatcher.getProjectStats();
        dispatcher.getBugCollection();
        dispatcher.logError("dispatchingMethod");
        dispatcher.logError("dispatchingMethod", new Exception());
        dispatcher.observeClass(DescriptorFactory.instance().getClassDescriptor("some/UnknownClass"));
        dispatcher.addObserver(bugInstance -> {
        });
        dispatcher.reportMissingClass(DescriptorFactory.instance().getClassDescriptor("some/UnknownClass"));
        dispatcher.reportMissingClass(new ClassNotFoundException());

        PrintStream stderr = System.err;
        try {
            System.setErr(UTF8.printStream(new ByteArrayOutputStream()));
            dispatcher.reportQueuedErrors();
            dispatcher.finish();
        } finally {
            System.setErr(stderr);
        }

        assertThat(bugReporter.getQueuedErrors().size(), is(2));
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
