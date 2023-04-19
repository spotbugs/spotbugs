package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindPublicAttributesTest extends AbstractIntegrationTest {
    private static final String PRIMITIVE_PUBLIC = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE";
    private static final String MUTABLE_PUBLIC = "PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE";
    private static final String ARRAY_PUBLIC = "PA_PUBLIC_ARRAY_ATTRIBUTE";

    @Test
    public void testPublicAttributesChecks() {
        performAnalysis("PublicAttributesTest.class");

        assertNumOfBugs(PRIMITIVE_PUBLIC, 3);
        assertNumOfBugs(ARRAY_PUBLIC, 3);
        assertNumOfBugs(MUTABLE_PUBLIC, 1);

        // The current implementation can only mark the correct line if the variable is initialized
        // at the field definition and the variable is not static
        assertBugTypeAtField(PRIMITIVE_PUBLIC, "attr1", 11);
        assertBugTypeAtField(PRIMITIVE_PUBLIC, "attr2");
        assertBugTypeAtField(PRIMITIVE_PUBLIC, "sattr1");
        assertBugTypeAtField(MUTABLE_PUBLIC, "hm", 20);
        assertBugTypeAtField(ARRAY_PUBLIC, "items", 27);
        assertBugTypeAtField(ARRAY_PUBLIC, "sitems");
        assertBugTypeAtField(ARRAY_PUBLIC, "SFITEMS");
    }

    @Test
    public void testGoodPublicAttributesChecks() {
        performAnalysis("PublicAttributesNegativeTest.class");
        assertNumOfBugs(PRIMITIVE_PUBLIC, 0);
        assertNumOfBugs(ARRAY_PUBLIC, 0);
        assertNumOfBugs(MUTABLE_PUBLIC, 0);
    }

    private void assertNumOfBugs(String bugtype, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBugTypeAtField(String bugtype, String field, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("PublicAttributesTest")
                .atField(field)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertBugTypeAtField(String bugtype, String field) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("PublicAttributesTest")
                .atField(field)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
