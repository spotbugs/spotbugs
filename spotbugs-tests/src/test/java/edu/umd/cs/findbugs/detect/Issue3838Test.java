package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3838Test extends AbstractIntegrationTest {

    @Test
    void testClassSuppression() {
        performAnalysis("ghIssues/issue3838/SuppressTimeFormatWorking.class");

        assertBugTypeCount("FS_BAD_DATE_FORMAT_FLAG_COMBO", 0);
    }

    @Test
    void testFieldSuppression() {
        performAnalysis("ghIssues/issue3838/SuppressTimeFormatBroken.class");

        assertBugAtLine("FS_BAD_DATE_FORMAT_FLAG_COMBO", 12);
        assertBugAtLine("FS_BAD_DATE_FORMAT_FLAG_COMBO", 17);

        assertBugTypeCount("FS_BAD_DATE_FORMAT_FLAG_COMBO", 2);
    }
}
