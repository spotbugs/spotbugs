package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class Issue2182Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue2182.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("SBSC_USE_STRINGBUFFER_CONCATENATION")
                .inClass("Issue2182")
                .inMethod("test")
                .atLine(22)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
