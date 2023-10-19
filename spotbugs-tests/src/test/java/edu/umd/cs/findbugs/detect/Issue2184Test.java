package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class Issue2184Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testIssue() {
        performAnalysis("../java17/ghIssues/Issue2184.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("PT_RELATIVE_PATH_TRAVERSAL")
                .inClass("Issue2184")
                .inMethod("test")
                .atLine(17)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
