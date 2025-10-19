package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2985Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2985.class",
                "ghIssues/Issue2985$NonStaticFactoryMethod.class",
                "ghIssues/Issue2985$PublicConstructor.class");

        assertBugInClassCount("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE", "ghIssues.Issue2985$NonStaticFactoryMethod", 0);
        assertBugInClassCount("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE", "ghIssues.Issue2985$PublicConstructor", 0);
    }
}
