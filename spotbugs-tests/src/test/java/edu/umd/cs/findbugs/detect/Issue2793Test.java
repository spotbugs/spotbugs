package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class Issue2793Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testRecordSerialVersionUid() {
        performAnalysis("../java17/ghIssues/Issue2793$RecordWithoutSerialVersionUid.class");

        assertBugCount("SE_NO_SERIALVERSIONID", 0);
    }

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testExternalizableRecord() {
        performAnalysis("../java17/ghIssues/Issue2793$ExternalizableRecord.class");

        assertBugCount("SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION", 0);
    }

    private void assertBugCount(String type, int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .build();

        assertThat(getBugCollection(), containsExactly(expectedCount, bugMatcher));
    }

    private void assertBugCount(int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .build();

        assertThat(getBugCollection(), containsExactly(expectedCount, bugMatcher));
    }
}
