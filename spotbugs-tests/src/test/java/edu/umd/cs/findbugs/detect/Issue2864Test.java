package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @author gtoison
 *
 */
class Issue2864Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../../../../src/classSamples/classEnhanceByHibernate/HibernateEnhancedEntity.class");
        assertNoBugType("DLS_DEAD_LOCAL_STORE");
    }
}
