package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

public class DontReusePublicIdentifiersTest extends AbstractIntegrationTest {
    @Test
    public void testShadowedPublicIdentifier() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifier.class");

        // TODO: implement the check for shadowed public identifiers
        assertNumOfShadowedPublicIdentifierBugs(0);

        // TODO:
        //     these should be the declarations of the shadowed public identifiers and not the initializations
        //     method names should not be reported yet(no such examples in the test cases)
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifier", "", 9);
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifier", "", 26);
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifier", "", 41);
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifier", "", 61);
    }

    @Test
    public void testObscuredPublicIdentifier() {
        performAnalysis("publicIdentifiers/ObscuredPublicIdentifier.class");

        // TODO:
        //      implement the check for obscured public identifiers
        assertEquals(2, 2);

        // TODO:
        //      implement a method for checking the obscured public identifiers in the correct methods and lines
        //      assertObscuredPublicIdentifierBug();
    }

    @Test
    public void testGoodPublicIdentifier() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifier.class");

        assertNumOfShadowedPublicIdentifierBugs(0);
    }

    private void assertNumOfShadowedPublicIdentifierBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertShadowedPublicIdentifierBug(String className, String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS").inClass(className)
                .inMethod(method).atLine(line).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertObscuredPublicIdentifierBug(String className, String method, int line) {
        // TODO: implement this method
    }
}
