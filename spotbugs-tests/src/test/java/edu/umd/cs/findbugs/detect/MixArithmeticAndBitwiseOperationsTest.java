package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class MixArithmeticAndBitwiseOperationsTest extends AbstractIntegrationTest {

    private static final String MABO_BUG_TYPE = "MABO_MIXING_ARITHMETIC_AND_BITWISE_OPERATIONS";

    @Test
    void testMixArithmeticAndBitwiseOperations() {
        performAnalysis("MixArithmeticAndBitwise.class");

        assertBugTypeCount(MABO_BUG_TYPE, 11);

        final String className = "MixArithmeticAndBitwise";

        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testMathThenBitwise", 10);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testBitwiseThenMath", 16);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testShiftOnMath", 22);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testUnaryMinusOnBitwise", 28);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testIincOnBitwise", 34);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testCastBetweenOperations", 42);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testMergePaths", 53);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testNoncompliant1", 58);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testNoncompliant2", 65);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testNoncompliant5", 74);
        assertBugInMethodAtLine(MABO_BUG_TYPE, className, "testNoncompliant6", 84);
    }
}
