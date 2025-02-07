package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SortedBugCollection;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class Issue641Test extends AbstractIntegrationTest {

    @Test
    void testUselessSuppression() {
        performAnalysis("suppression/Issue641.class");
        SortedBugCollection bugCollection = (SortedBugCollection) getBugCollection();

        assertBugAtField("US_USELESS_SUPPRESSION_ON_FIELD", "suppression.Issue641", "field");
        assertBugInClass("US_USELESS_SUPPRESSION_ON_CLASS", "suppression.Issue641");
        assertBugInMethod("US_USELESS_SUPPRESSION_ON_METHOD_PARAMETER", "suppression.Issue641", "setField");

        assertThat("Expected three warnings",
                bugCollection.getCollection().size(),
                CoreMatchers.equalTo(3));
    }
}
