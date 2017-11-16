package edu.umd.cs.findbugs.integration;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/78">GitHub
 *      issue</a>
 */
public class Issue78Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue0078.class");
    }
}
