package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090205Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_02_05.class");
        // FPs in isDecember in line 24, 28
        // FPs in isBefore2009 (line 12), is2009 (line 16), is2008 (line 20), but it's difficult to know the programmer's intent,
        //  also the used function is already deprecated because of its confusing design

        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("DMI_BAD_MONTH")
                .inClass("Ideas_2009_02_05")
                .inMethod("endDate")
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
