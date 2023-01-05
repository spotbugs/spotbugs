package edu.umd.cs.findbugs.detect.MyTest;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

/**
 * @author Lin
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/374">GitHub issue</a>
 */
public class Issue675Test extends AbstractIntegrationTest {
    /**
     * When there are unread private fields or unused private fields
     * in a class, SpotBugs should report the bug for unread private fields
     * and unused private fields in the class.
     */
    @Test
    public void test() {
        performAnalysis("ghIssues/issue675/Test.class");
        BugInstanceMatcher bugTypeMatcher1 = new BugInstanceMatcherBuilder().bugType("URF_UNREAD_FIELD").build();
        BugInstanceMatcher bugTypeMatcher2 = new BugInstanceMatcherBuilder().bugType("UUF_UNUSED_FIELD").build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher1));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher2));
    }

    /**
     * When there are unread private fields or unused private fields
     * in a subclass, SpotBugs should report the bug for unread private fields
     * and unused private fields in the class.
     */
    @Test
    public void test1() {
        performAnalysis("ghIssues/issue675/Test_1.class");
        BugInstanceMatcher bugTypeMatcher1 = new BugInstanceMatcherBuilder().bugType("URF_UNREAD_FIELD").build();
        BugInstanceMatcher bugTypeMatcher2 = new BugInstanceMatcherBuilder().bugType("UUF_UNUSED_FIELD").build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher1));
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher2));
    }

    /**
     * When there are unread private fields or unused private fields
     * in a subclass, SpotBugs should report the bug for unread private fields
     * and unused private fields in the class. When there are no unread private
     * fields or unused private fields in the superclass of it, SpotBugs should
     * report nothing.
     */
    @Test
    public void test2(){
        performAnalysis("ghIssues/issue675/Test_issue675_father.class",
                "ghIssues/issue675/Test_issue675.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("URF_UNREAD_FIELD").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }
}
