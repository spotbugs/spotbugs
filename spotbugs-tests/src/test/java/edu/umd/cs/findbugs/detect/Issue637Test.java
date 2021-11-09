package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * SpotBugs should check for bad combinations of date format flags in SimpleDateFormat
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/637">GitHub issue</a>
 * @since 4.4.2
 */
public class Issue637Test extends AbstractIntegrationTest {
    private static final String DESIRED_BUG_TYPE = "FS_BAD_DATE_FORMAT_FLAG_COMBO";

    @Test
    public void testClass() {
        performAnalysis("ghIssues/Issue637.class");
        BugInstanceMatcher bugTypeMatcherA = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(22)
                .build();
        BugInstanceMatcher bugTypeMatcherB = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(34)
                .build();
        BugInstanceMatcher bugTypeMatcherC = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(46)
                .build();

        BugInstanceMatcher bugTypeMatcherD = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(28)
                .build();
        BugInstanceMatcher bugTypeMatcherE = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(40)
                .build();
        BugInstanceMatcher bugTypeMatcherF = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(52)
                .build();

        BugInstanceMatcher bugTypeMatcherG = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(59)
                .build();

        BugInstanceMatcher bugTypeMatcherH = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(66)
                .build();

        BugInstanceMatcher bugTypeMatcherI = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .atLine(73)
                .build();

        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherA));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherB));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherC));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherG));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherH));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherI));

        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherD));
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherE));
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherF));

    }



}
