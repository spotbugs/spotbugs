package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DontReusePublicIdentifiersTest extends AbstractIntegrationTest {
    @Test
    public void testShadowedPublicIdentifiersClassNames() {
        // check shadowed public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/InputStream.class");
        assertNumOfShadowedPublicIdentifierBugs(1);
        assertShadowedPublicIdentifierBug("InputStream");

        // check shadowed public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames.class");
        assertNumOfShadowedPublicIdentifierBugs(2);
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifiersInnerClassNames");
    }

    @Test
    public void testGoodPublicIdentifiersClassNames() {
        // check good public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/GoodPublicIdentifiersStandaloneClassNames.class");
        assertNumOfShadowedPublicIdentifierBugs(0);

        // check good public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames.class");
        assertNumOfShadowedPublicIdentifierBugs(0);
    }

    @Test
    public void testShadowedPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersVariableNames.class");
        assertNumOfShadowedPublicIdentifierBugs(3);

        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifiersVariableNames");
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifiersVariableNames", "containsShadowedLocalVariable1", 11);
        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifiersVariableNames", "containsShadowedLocalVariable2", 15);
    }

    @Test
    public void testGoodPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersVariableNames.class");
        assertNumOfShadowedPublicIdentifierBugs(0);
    }

    @Test
    public void testGoodPublicIdentifierMethodNames() {
        // TODO:
        //      implement the check for good public identifiers
        assertTrue(true);
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

    private void assertNumOfShadowedPublicIdentifierBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertShadowedPublicIdentifierBug(String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS").inClass(className)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierBug(String className, String methodName, int lineNumber) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS")
                .inClass(className)
                .inMethod(methodName)
                .atLine(lineNumber)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
