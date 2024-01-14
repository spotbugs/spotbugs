package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.SpotBugsExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

/**
 * @author gtoison
 *
 */
@ExtendWith(SpotBugsExtension.class)
class Issue2759Test extends AbstractIntegrationTest {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/src/classSamples/classesModifiedByJacoco/JavaLanguageParser$ClassBlockContext.class"));
    }
}
