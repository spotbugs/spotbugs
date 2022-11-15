package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class Issue2258Test extends AbstractIntegrationTest {

	@Test
	public void shouldNotFindSelfAssignment() {
		performAnalysis("ghIssues/Issue2258.class");
		BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
				.bugType("SA_FIELD_SELF_ASSIGNMENT")
				.inClass("Issue2258")
				.build();
		assertThat(getBugCollection(), containsExactly(0, matcher));
	}
}
