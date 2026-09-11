package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2749Test extends AbstractIntegrationTest {
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testIssue() {
        performAnalysis(
                "../java11/ghIssues/Issue2749$WithMethodHandles.class",
                "../java11/ghIssues/Issue2749$WithVarHandle$Value.class",
                "../java11/ghIssues/Issue2749$WithVarHandle.class",
                "../java11/ghIssues/Issue2749.class");
        assertThat(getBugCollection(), is(emptyIterable()));
    }
}
