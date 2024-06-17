package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue2120Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testIssue() {
        performAnalysis("../java17/Issue2120.class",
                "../java17/Issue2120$1MyEnum.class",
                "../java17/Issue2120$1MyRecord.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS").build();
        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
