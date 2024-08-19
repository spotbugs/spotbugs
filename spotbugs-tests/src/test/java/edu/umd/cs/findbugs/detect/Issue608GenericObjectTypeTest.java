package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue608GenericObjectTypeTest extends AbstractIntegrationTest {

    @Test
    void testIssue() {
    	performAnalysis("ghIssues/Issue608GenericObjectType.class");
    }
}
