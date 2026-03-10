package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class Issue3485Test extends AbstractIntegrationTest {
    @Test
    void testMethodNameForReturnValue() {
        performAnalysis("ghIssues/Issue3485.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 2);
        assertBugInMethodAtVariable("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue3485", "bar", "id");
        assertThat("nullResult method call in foo", getBugCollection().getCollection().stream().anyMatch(bug -> {
            final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                    .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
                    .inClass("ghIssues.Issue3485")
                    .inMethod("foo")
                    .build();
            if (matcher.matches(bug)) {
                MethodAnnotation call = bug.getAnnotationWithRole(MethodAnnotation.class, MethodAnnotation.METHOD_CALLED);
                return call != null && call.getMethodName().equals("nullReturn");
            }
            return false;
        }));
    }
}
