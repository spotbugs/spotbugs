package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ErrorCountingBugReporterTest {

    @Test
    void reportMissingClassByClassNotFoundException() {
        PrintingBugReporter realBugReporter = new PrintingBugReporter();
        ErrorCountingBugReporter bugReporter = new ErrorCountingBugReporter(realBugReporter);
        ClassNotFoundException ex = new ClassNotFoundException("Class org.example.Foo cannot be resolved.");
        bugReporter.reportMissingClass(ex);
        assertThat(bugReporter.getMissingClassCount(), is(1));
    }

    @Test
    void reportMissingClassByClassDescriptor() {
        PrintingBugReporter realBugReporter = new PrintingBugReporter();
        ErrorCountingBugReporter bugReporter = new ErrorCountingBugReporter(realBugReporter);
        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptorFromDottedClassName("org.example.Bar");
        bugReporter.reportMissingClass(classDescriptor);
        assertThat(bugReporter.getMissingClassCount(), is(1));
    }
}
