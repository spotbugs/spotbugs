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

        assertNumOfShadowedPublicIdentifierBugs(1);

        assertShadowedPublicIdentifierBug("ShadowedPublicIdentifier", "ShadowedVector", 14);
    }

    @Test
    public void testObscuredPublicIdentifier() {
        performAnalysis("publicIdentifiers/ObscuredPublicIdentifier.class");

        assertEquals(1, 1);
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
}
