package edu.umd.cs.findbugs.detect;

import static org.junit.Assert.*;

import org.junit.*;

import edu.umd.cs.findbugs.*;

/**
 * {@link ghIssues.Issue1415}
 * 
 * @see {@link ForgotToUpdateHashCodeEqualsToStringDetector}
 * @author lifeinwild1@gmail.com https://github.com/lifeinwild
 */
public class Issue1415Test extends AbstractIntegrationTest {
    private void asserts(boolean memberDoesntAppearInEquals, boolean memberDoesntAppearInHashcode,
            boolean memberDoesntAppearInTostring, boolean noEqualsUnlikeParent, boolean noHashcodeUnlikeParent,
            boolean noTostringUnlikeParent) {
        BugInstance[] bugs = getBugs();
        assertEquals(noEqualsUnlikeParent, has(bugs,
                ForgotToUpdateHashCodeEqualsToStringDetector.HE_NO_EQUALS_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE));
        assertEquals(noHashcodeUnlikeParent, has(bugs,
                ForgotToUpdateHashCodeEqualsToStringDetector.HE_NO_HASHCODE_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE));
        assertEquals(noTostringUnlikeParent, has(bugs,
                ForgotToUpdateHashCodeEqualsToStringDetector.USELESS_STRING_NO_TOSTRING_UNLIKE_PARENT_OR_OUTER_CLASS_OR_INTERFACE));
        assertEquals(memberDoesntAppearInEquals,
                has(bugs, ForgotToUpdateHashCodeEqualsToStringDetector.HE_MEMBER_DOESNT_APPEAR_IN_EQUALS));
        assertEquals(memberDoesntAppearInHashcode,
                has(bugs, ForgotToUpdateHashCodeEqualsToStringDetector.HE_MEMBER_DOESNT_APPEAR_IN_HASHCODE));
        assertEquals(memberDoesntAppearInTostring, has(bugs,
                ForgotToUpdateHashCodeEqualsToStringDetector.USELESS_STRING_MEMBER_DOESNT_APPEAR_IN_TOSTRING));
    }

    private BugInstance[] getBugs() {
        SortedBugCollection bugs = (SortedBugCollection) getBugCollection();
        BugInstance[] sorted = new BugInstance[bugs.getCollection().size()];
        bugs.getCollection().toArray(sorted);
        return sorted;
    }

    private boolean has(BugInstance[] bugs, String bugType) {
        for (BugInstance bug : bugs) {
            if (bug.getType().equals(bugType)) {
                return true;
            }
        }
        return false;
    }

    private void perform(String... analyzeMe) {
        performAnalysis(Priorities.IGNORE_PRIORITY, BugRanker.VISIBLE_RANK_MAX, analyzeMe);
    }

    @Test
    public void testChildNoInstanceVar() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringParent.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringChildNoInstanceVar.class");
        asserts(false, false, false, true, true, true);
    }

    @Test
    public void testChildNoMethod() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringChildNoMethod.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringParent.class");
        asserts(false, false, false, true, true, true);
    }

    @Test
    public void testDummyMethod() {
        perform("ghIssues/Issue1415$HETMethodsAsDummy.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testGetter() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringGetter.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testInner() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringOuter$ForgotToUpdateHashcodeEqualsToStringInner.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringOuter.class");
        asserts(false, false, false, true, true, true);
    }

    @Test
    public void testInner2() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringOuter$ForgotToUpdateHashcodeEqualsToStringInner2.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringOuter.class");
        asserts(true, true, true, false, false, false);
    }

    @Test
    public void testInner3() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringOuter2$ForgotToUpdateHashcodeEqualsToStringInner3.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringOuter2.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testInterface() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringInterface.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testNoMethod() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringNoMethod.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testNoMethodImplementation() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringImplementation.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringInterface.class");
        asserts(false, false, false, true, true, true);
    }

    @Test
    public void testNormal() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringNormal.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testNotAppear() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringNotAppear.class");
        asserts(true, true, true, false, false, false);
    }

    @Test
    public void testParent() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringParent.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testProperChild() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringNormal.class",
                "ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringProperChild.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testStatic() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringStatic.class");
        asserts(false, false, false, false, false, false);
    }

    @Test
    public void testTransient() {
        perform("ghIssues/Issue1415$ForgotToUpdateHashcodeEqualsToStringTransient.class");
        asserts(false, false, false, false, false, false);
    }
}
