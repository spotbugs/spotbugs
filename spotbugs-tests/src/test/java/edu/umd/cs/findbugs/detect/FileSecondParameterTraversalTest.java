package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class FileSecondParameterTraversalTest extends AbstractIntegrationTest {

    @Test
    void fileConstructorsWithABaseArePathTraversalSinks() {
        performAnalysis("../java17/ghIssues/FileSecondParameterTraversal.class");

        assertBugInMethod("PT_ABSOLUTE_PATH_TRAVERSAL", "FileSecondParameterTraversal", "direct");
        assertBugInMethod("PT_ABSOLUTE_PATH_TRAVERSAL", "FileSecondParameterTraversal", "underBaseString");
        assertBugInMethod("PT_ABSOLUTE_PATH_TRAVERSAL", "FileSecondParameterTraversal", "underBaseFile");
    }
}
