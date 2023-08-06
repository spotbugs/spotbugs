/**
 * 
 */
package edu.umd.cs.findbugs.detect;


import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author gtoison
 *
 */
public class NullPointerDereferenceInLambdaTest extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis(
                "ghIssues/NullPointerDereferenceInLambda.class",
                "ghIssues/NullPointerDereferenceInLambda$1.class",
                "ghIssues/NullPointerDereferenceInLambda$1$1.class");

        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_ALWAYS_NULL")
                .atLine(8)
                .build();

        assertThat(getBugCollection(), containsExactly(1, matcher));
    }
}
