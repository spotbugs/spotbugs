package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.ba.Dataflow;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3785">GitHub issue</a>
 */
class Issue3785Test extends AbstractIntegrationTest {

    @Test
    void testAnalysisWithDataflowDebug() {
        // Dataflow.DEBUG is static and shared by all tests running in this JVM, so always restore it.
        boolean debugWas = Dataflow.DEBUG;
        Dataflow.DEBUG = true;
        try {
            // performAnalysis() fails if any analysis error is reported
            performAnalysis("ghIssues/Issue3785.class");
        } finally {
            Dataflow.DEBUG = debugWas;
        }

        // FindBadCast2 needs the type narrowed by the instanceof check inside the loop
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "Issue3785", "impossibleCastInLoop", 23);
    }
}
