package edu.umd.cs.findbugs.detect;

import static org.junit.Assert.assertThat;

import static org.hamcrest.core.Is.is;

import static org.hamcrest.collection.IsEmptyIterable.*;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

public class FindUnsatisfiedObligationTest extends AbstractIntegrationTest {
    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/60">GitHub
     *      issue</a>
     */
    @Test
    public void testIssue60() {
        performAnalysis("Issue60.class");
        assertThat(getBugCollection(), is(emptyIterable()));
    }
}
