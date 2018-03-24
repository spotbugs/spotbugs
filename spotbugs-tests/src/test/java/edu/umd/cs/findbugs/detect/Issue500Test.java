package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/500">GitHub issue</a>
 * @since 3.1
 */
public class Issue500Test extends AbstractIntegrationTest {
  @Test
  public void test() {
    assumeThat(System.getProperty("java.specification.version"), is("9"));

    performAnalysis("../java9/Issue500.class");
    final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().build();
    assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
  }
}
