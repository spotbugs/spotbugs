package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Issue1147Test extends AbstractIntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "ghIssues/Issue1147.class",
        "../../../../src/classSamples/compiledWithCorretto11.0.4_10/Issue1147.class"
    })
    void testIssue(String path) {
        performAnalysis(path);

        assertBugTypeCount("DMI_INVOKING_TOSTRING_ON_ARRAY", 1);
    }
}
