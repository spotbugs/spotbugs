package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;


public class HidingSubClassTest extends AbstractIntegrationTest {
    @Test
    public void testBadFindHidingSubClassTest() {
        performAnalysis("FindHidingSubClassTest/BadFindHidingSubClassTest.class",
                "FindHidingSubClassTest/GrantAccessStatic.class",
                "FindHidingSubClassTest/GrantUserAccessStatic.class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("HSBC_FIND_HIDING_SUB_CLASS")
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void testGoodFindHidingSubClassTest() {
        performAnalysis("FindHidingSubClassTest/GoodFindHidingSubClassTest.class",
                "FindHidingSubClassTest/GrantAccess.class",
                "FindHidingSubClassTest/GoodFindHidingSubClassTest.class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("HSBC_FIND_HIDING_SUB_CLASS")
                .inMethod("main")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
