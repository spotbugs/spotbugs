package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ReadReturnShouldBeCheckedTest extends AbstractIntegrationTest {

    private static final String NCR_BUG_TYPE = "NCR_NOT_PROPERLY_CHECKED_READ";
    private static final String RR_BUG_TYPE = "RR_NOT_CHECKED";
    private static final String SR_BUG_TYPE = "SR_NOT_CHECKED";

    @Test
    void testNotCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/BadReadReturnShouldBeCheckedTest.class");

        assertBugTypeCount(NCR_BUG_TYPE, 6);
        assertBugTypeCount(RR_BUG_TYPE, 0);
        assertBugTypeCount(SR_BUG_TYPE, 0);

        final String className = "BadReadReturnShouldBeCheckedTest";
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readBytes", 14);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readBytesWithOffset", 23);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readFromBufferedReader", 32);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "readBytesComparedToFloat", 40);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "changeOrderOfComparing", 48);
        assertBugInMethodAtLine(NCR_BUG_TYPE, className, "changeOrderOfComparingLong", 56);
    }

    @Test
    void testCheckedRead() {
        performAnalysis("partialFilledArrayWithRead/GoodReadReturnShouldBeCheckedTest.class");

        assertNoBugType(NCR_BUG_TYPE);
        assertNoBugType(RR_BUG_TYPE);
        assertNoBugType(SR_BUG_TYPE);
    }
}
