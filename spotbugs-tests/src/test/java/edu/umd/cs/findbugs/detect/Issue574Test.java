package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * SpotBugs should suppress "UNF_UNREAD_FIELD" warning when triggering fields have certain annotations.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
 */
class Issue574Test extends AbstractIntegrationTest {
    private static final String DESIRED_BUG_TYPE = "URF_UNREAD_FIELD";

    /**
     * Expects 0 "URF_UNREAD_FIELD" errors in the "Issue574SerializedName.class" file.
     * Tests <code>@SerializedName</code> annotation.
     */
    @Test
    void testSerializedNameAnnotation() {
        performAnalysis("ghIssues/Issue574SerializedName.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    /**
     * Expects 0 "URF_UNREAD_FIELD" errors in the "Issue574XmlElement.class" file.
     * Tests <code>@XmlElement</code>, <code>@XmlAttribute</code>, and <code>@XmlValue</code> annotations.
     */
    @Test
    void testXmlElementAnnotation() {
        performAnalysis("ghIssues/Issue574XmlElement.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    /**
     * Expects 0 "URF_UNREAD_FIELD" errors in the "Issue574RegisterExtension.class" file.
     * Tests <code>@RegisterExtension</code> annotation.
     */
    @Test
    void testRegisterExtensionAnnotation() {
        performAnalysis("ghIssues/Issue574RegisterExtension.class",
                "ghIssues/BypassFileNotFoundExceptionExtension.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
