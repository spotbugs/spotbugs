package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1155">GitHub issue</a>
 */
public class Issue1155Test extends AbstractIntegrationTest {
    @Test
    public void testIssue1155() {
        performAnalysis("ghIssues/Issue1155.class");
        assertThat(getBugCollection(), is(emptyIterable()));
    }
}
