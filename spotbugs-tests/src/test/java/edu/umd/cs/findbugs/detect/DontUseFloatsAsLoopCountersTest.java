package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class DontUseFloatsAsLoopCountersTest extends AbstractIntegrationTest {

    @Test
    void testChecks() {
        performAnalysis("DontUseFloatsAsLoopCounters.class");
        assertBugTypeCount("FL_FLOATS_AS_LOOP_COUNTERS", 3);

        assertBugInMethodAtLine("FL_FLOATS_AS_LOOP_COUNTERS", "DontUseFloatsAsLoopCounters", "test1", 8);
        assertBugInMethodAtLine("FL_FLOATS_AS_LOOP_COUNTERS", "DontUseFloatsAsLoopCounters", "test2", 15);
        assertBugInMethodAtLine("FL_FLOATS_AS_LOOP_COUNTERS", "DontUseFloatsAsLoopCounters", "test3", 21);
    }
}
