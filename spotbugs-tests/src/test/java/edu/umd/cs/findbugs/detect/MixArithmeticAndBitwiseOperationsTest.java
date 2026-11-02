package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class MixArithmeticAndBitwiseOperationsTest extends AbstractIntegrationTest {

    @Test
    void testMixArithmeticAndBitwiseOperations() {
        final String bugType = "MABO_MIXING_ARITHMETIC_AND_BITWISE_OPERATIONS";
        performAnalysis("MixArithmeticAndBitwise.class");

        assertBugTypeCount(bugType, 15);

        final String className = "MixArithmeticAndBitwise";

        assertBugInMethodAtLine(bugType, className, "testMathThenBitwise", 10);
        assertBugInMethodAtLine(bugType, className, "testBitwiseThenMath", 16);
        assertBugInMethodAtLine(bugType, className, "testShiftOnMath", 22);
        assertBugInMethodAtLine(bugType, className, "testUnaryMinusOnBitwise", 28);
        assertBugInMethodAtLine(bugType, className, "testIincOnBitwise", 34);
        assertBugInMethodAtLine(bugType, className, "testCastBetweenOperations", 42);
        assertBugInMethodAtLine(bugType, className, "testMergePaths", 53);
        assertBugInMethodAtLine(bugType, className, "testNoncompliant1", 58);
        assertBugInMethodAtLine(bugType, className, "testNoncompliant2", 65);
        assertBugInMethodAtLine(bugType, className, "testNoncompliant5", 74);
        assertBugInMethodAtLine(bugType, className, "testNoncompliant6", 84);
        assertBugInMethodAtLine(bugType, className, "testSameInputInSeparateExpressions", 169);
        assertBugInMethod(bugType, className, "testReportOnceForSameValue");
        assertBugInMethod(bugType, className, "testLongMathThenBitwise");
        assertBugInMethod(bugType, className, "testLongBitwiseThenMath");

        assertNoBugInMethod(bugType, className, "testPureMath");
        assertNoBugInMethod(bugType, className, "testPureBitwise");
        assertNoBugInMethod(bugType, className, "testBitwiseNot");
        assertNoBugInMethod(bugType, className, "testShiftAmountIsMath");
        assertNoBugInMethod(bugType, className, "testVariableReuse");
        assertNoBugInMethod(bugType, className, "testLiteralsOnly");
        assertNoBugInMethod(bugType, className, "testSlotReuseForIINC");
        assertNoBugInMethod(bugType, className, "testUntaggedCast");
        assertNoBugInMethod(bugType, className, "testCompliant1");
        assertNoBugInMethod(bugType, className, "testCompliant3");
        assertNoBugInMethod(bugType, className, "testSeparateFields");
        assertNoBugInMethod(bugType, className, "testLongShiftAmountIsMath");
    }
}
