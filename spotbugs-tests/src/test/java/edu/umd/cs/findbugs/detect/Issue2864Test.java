package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

/**
 * @author gtoison
 *
 */
@ExtendWith(SpotBugsExtension.class)
class Issue2864Test extends AbstractIntegrationTest {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/src/classSamples/classEnhanceByHibernate/HibernateEnhancedEntity.class"));

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DLS_DEAD_LOCAL_STORE")
                .build();

        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));
    }
}
