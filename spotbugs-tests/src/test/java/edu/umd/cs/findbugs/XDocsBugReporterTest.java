package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class XDocsBugReporterTest {
    @Test
    void reportMissingClassByClassNotFoundException() {
        Project project = new Project();
        XDocsBugReporter bugReporter = new XDocsBugReporter(project);
        ClassNotFoundException ex = new ClassNotFoundException("Class org.example.Foo cannot be resolved.");
        bugReporter.reportMissingClass(ex);
        SortedBugCollection bugCollection = (SortedBugCollection) bugReporter.getBugCollection();
        assertThat(bugCollection.missingClassIterator().next(), is("org.example.Foo"));
    }

    @Test
    void reportMissingClassByClassDescriptor() {
        Project project = new Project();
        XDocsBugReporter bugReporter = new XDocsBugReporter(project);
        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptorFromDottedClassName("org.example.Bar");
        bugReporter.reportMissingClass(classDescriptor);
        SortedBugCollection bugCollection = (SortedBugCollection) bugReporter.getBugCollection();
        assertThat(bugCollection.missingClassIterator().next(), is("org.example.Bar"));
    }
}
