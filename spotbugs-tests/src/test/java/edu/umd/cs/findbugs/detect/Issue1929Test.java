package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1929Test extends AbstractIntegrationTest {

    @Test
    void testHardcodedAbsoluteFilenameWithConcat() {
        performAnalysis("ghIssues/Issue1929.class");
        assertBugInMethodAtLine("DMI_HARDCODED_ABSOLUTE_FILENAME", "ghIssues.Issue1929", "hardcodedAbsoluteFilenameWithConcat",
                8);
    }

    @Test
    void testExtractMakeConcatRecipe() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new String[] { "/tmp/fswebcam-pipe-", ".mjpeg" },
                DumbMethodInvocations.literalPartsFromConcatRecipe("/tmp/fswebcam-pipe-\u0001.mjpeg"));
    }
}
