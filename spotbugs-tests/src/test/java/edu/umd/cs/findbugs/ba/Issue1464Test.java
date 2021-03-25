package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class Issue1464Test extends AbstractIntegrationTest {

   @Test
   public void test() {
       performAnalysis("ghIssues/Issue1464.class");
       BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("DMI_RANDOM_USED_ONLY_ONCE").build();
       assertThat(getBugCollection(), containsExactly(2, bugTypeMatcher));
   }

}
