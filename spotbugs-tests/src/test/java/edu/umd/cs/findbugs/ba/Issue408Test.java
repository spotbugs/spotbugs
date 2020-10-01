package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/408">#408</a>
 * @since 3.1
 */
public class Issue408Test extends AbstractIntegrationTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testSingleClass() {
        expected.expect(AssertionError.class);
        expected.expectMessage("Analysis failed with exception");
        expected.expectCause(is(instanceOf(IOException.class)));
        performAnalysis("../java11/module-info.class");
    }

    @Test
    public void testFewClasses() {
        performAnalysis("../java11/module-info.class", "../java11/Issue408.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

}
