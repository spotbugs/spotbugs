package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

public class Issue641Test extends AbstractIntegrationTest {

    @Test
    public void testUselessSuppression() {

        performAnalysis("/suppression/Issue641.class");
        SortedBugCollection bugCollection = (SortedBugCollection) getBugCollection();

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("US_USELESS_SUPPRESSION_ON_FIELD")
                .inClass("suppression.Issue641")
                .atField("field")
                .build();
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("US_USELESS_SUPPRESSION_ON_CLASS")
                .inClass("suppression.Issue641")
                .build();
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("US_USELESS_SUPPRESSION_ON_METHOD_PARAMETER")
                .inClass("suppression.Issue641")
                .inMethod("setField")
                .build();
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));

        assertThat("Expected three warnings",
                bugCollection.getCollection().size(),
                CoreMatchers.equalTo(3));
    }
}
