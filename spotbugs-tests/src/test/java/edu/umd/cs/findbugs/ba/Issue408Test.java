package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/408">#408</a>
 * @since 3.1
 */
class Issue408Test extends AbstractIntegrationTest {

    @Test
    void testSingleClass() {
        Throwable throwable = Assertions.assertThrows(AssertionError.class, () -> {
            performAnalysis("../java11/module-info.class");
        });
        Assertions.assertEquals("Analysis failed with exception", throwable.getMessage());
        assertThat(throwable.getCause(), is(instanceOf(IOException.class)));
    }

    @Test
    void testFewClasses() {
        performAnalysis("../java11/module-info.class", "../java11/Issue408.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

}
