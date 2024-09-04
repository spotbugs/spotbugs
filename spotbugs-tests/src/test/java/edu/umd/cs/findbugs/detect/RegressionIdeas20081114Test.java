package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionIdeas20081114Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2008_11_14.class");

        assertBugNum("GC_UNRELATED_TYPES", 3, 9);
        assertExactBugNum("EC_UNRELATED_CLASS_AND_INTERFACE", 1);
        assertExactBugNum("EC_UNRELATED_TYPES", 2);
        
        assertBug("GC_UNRELATED_TYPES","Ideas_2008_11_14","foo",32);
        assertBug("GC_UNRELATED_TYPES","Ideas_2008_11_14","foo",33);
        assertBug("GC_UNRELATED_TYPES","Ideas_2008_11_14","foo",34);
        assertBug("EC_UNRELATED_CLASS_AND_INTERFACE", "Ideas_2008_11_14", "foo", 36);
        // assertBug("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 37); // FN
        assertBug("EC_UNRELATED_TYPES","Ideas_2008_11_14","foo", 38);
        assertBug("EC_UNRELATED_TYPES","Ideas_2008_11_14","foo",39);

        // assertBug("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 17); // FN
        // assertBug("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 18); // FN
        // assertBug("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 19); // FN
        // assertBug("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 20); // FN
        // assertBug("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 21); // FN
    }

    private void assertExactBugNum(String bugtype, int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertBugNum(String bugtype, int min, int max) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        long exactMatches = getBugCollection().getCollection().stream().filter(matcher::matches).count();
        assertTrue(exactMatches >= min);
        assertTrue(exactMatches <= max);
    }
    
    private void assertBug(String bugtype, String className, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass(className)
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
