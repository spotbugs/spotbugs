package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindArgumentAssertionsTest extends AbstractIntegrationTest {
    @Test
    public void testArgumentAssertions() {
        performAnalysis("ArgumentAssertions.class");

        assertNumOfDABugs(23);

        assertDABug("getAbsAdd", 4);
        assertDABug("getAbsAdd", 5);
        assertDABug("getAbsAdd2", 14);
        assertDABug("getAbsAdd2", 15);
        assertDABug("getAbsAdd3", 28);
        assertDABug("getAbsAdd3", 29);
        assertDABug("getAbsAdd3", 30);
        assertDABug("getAbsAdd4", 39);
        assertDABug("print", 47);
        assertDABug("indirect", 59);
        assertDABug("compBool1", 64);
        assertDABug("compBool2", 70);
        assertDABug("getAbs", 76);
        assertDABug("getAbs", 83);
        assertDABug("compShor", 90);
        assertDABug("getAbs", 96);
        assertDABug("compLong", 103);
        assertDABug("getAbs", 109);
        assertDABug("compFloat", 116);
        assertDABug("getAbs", 122);
        assertDABug("compDouble", 129);
        assertDABug("indirect", 136);
        // assertDABug("indirect2", 143); -- indirect assertions of arguments are not supported yet (except copies)
        assertNoDABug("literal");
        assertNoDABug("literalAndMessage");
        assertNoDABug("literalAndMessageStr");
        assertNoDABug("conditionallyInMessage");
        assertNoDABug("privateMethod");
        assertNoDABug("privateFinalMethod");
        assertNoDABug("privateStaticMethod");
        assertDABug("assertingArgInFor", 198);
        // assertDABug("lambda$assertingArgInStream$0", 206); // assertations inside streams are not supported yet
    }

    private void assertNumOfDABugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("AA_ASSERTION_OF_ARGUMENTS").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertNoDABug(String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("AA_ASSERTION_OF_ARGUMENTS")
                .inClass("ArgumentAssertions")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugInstanceMatcher));
    }

    private void assertDABug(String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("AA_ASSERTION_OF_ARGUMENTS")
                .inClass("ArgumentAssertions")
                .inMethod(method)
                .atLine(line)
                .withConfidence(Confidence.LOW)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
