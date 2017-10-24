package edu.umd.cs.findbugs.ba;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.NoClassesFoundToAnalyzeException;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/408">#408</a>
 * @since 3.1
 */
public class Issue408Test extends AbstractIntegrationTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void test() {
        assumeThat(System.getProperty("java.specification.version"), is("9"));

        expected.expect(AssertionError.class);
        expected.expectMessage("Analysis failed with exception");
        expected.expectCause(is(instanceOf(NoClassesFoundToAnalyzeException.class)));
        performAnalysis("../java9/module-info.class");
    }

}
