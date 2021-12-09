package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * SpotBugs should suppress "UNF_UNREAD_FIELD" warning when triggering fields have certain annotations.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
 * @since 4.4.2
 */
public class Issue574Test extends AbstractIntegrationTest {
    private static final String DESIRED_BUG_TYPE = "URF_UNREAD_FIELD";

    /**
     * Expects 0 "URF_UNREAD_FIELD" errors in the "Issue574SerializedName.class" file.
     */
    @Test
    public void testSerializedNameAnnotation() {
        performAnalysis("ghIssues/Issue574SerializedName.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    /**
     * Expects 2 "URF_UNREAD_FIELD" errors in the "Issue574XmlElement.class" file.
     */
    @Test
    public void testXmlElementAnnotation() {
        performAnalysis("ghIssues/Issue574XmlElement.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
