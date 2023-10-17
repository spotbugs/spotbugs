package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class Issue2183Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue2183.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
                .inClass("Issue2183")
                .inMethod("test")
                .atLine(11)
                .withConfidence(Confidence.HIGH)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
