package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class DumbMethodsAndInvocationsTest extends AbstractIntegrationTest {
    @Test
    void testCallSystemExit() {
        performAnalysis("CallSystemExit.class");

        assertBugTypeCount("DM_EXIT", 1);
        assertBugInMethod("DM_EXIT", "CallSystemExit", "equals");
    }

    @Test
    void testCloneStringArray() {
        performAnalysis("CloneStringArray.class");

        assertBugTypeCount("DMI_INVOKING_TOSTRING_ON_ARRAY", 1);
        assertBugInMethodAtLine("DMI_INVOKING_TOSTRING_ON_ARRAY", "CloneStringArray", "main", 4);
    }

    @Test
    void testInvokeGC() {
        performAnalysis("InvokeGC.class");

        assertBugTypeCountBetween("DM_GC", 3, 4);
        // FN in tricky
        assertBugInMethod("DM_GC", "InvokeGC", "bad");
        assertBugInMethod("DM_GC", "InvokeGC", "finalize");
        assertBugInMethod("DM_GC", "InvokeGC", "tricky2");
    }

    @Test
    void testNoopThread() {
        performAnalysis("NoopThread.class");

        assertBugTypeCount("DM_USELESS_THREAD", 3);
        assertBugInMethod("DM_USELESS_THREAD", "NoopThread", "test1");
        assertBugInMethod("DM_USELESS_THREAD", "NoopThread", "test2");
        assertBugInMethod("DM_USELESS_THREAD", "NoopThread", "test3");
    }

    @Test
    void testRandomStuff() {
        performAnalysis("RandomStuff.class");

        assertBugTypeCount("DM_NEXTINT_VIA_NEXTDOUBLE", 1);
        assertBugTypeCount("RV_01_TO_INT", 1);
        assertBugInMethodAtLine("DM_NEXTINT_VIA_NEXTDOUBLE", "RandomStuff", "screwAround", 17);
        assertBugInMethodAtLine("RV_01_TO_INT", "RandomStuff", "screwAround", 38);
    }
}
