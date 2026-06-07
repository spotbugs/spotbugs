public class MixArithmeticAndBitwise {

    // =========================================================================
    // POSITIVE TEST CASES (VIOLATING - Should report a bug)
    // =========================================================================

    // TP1: Using an arithmetic result in a bitwise operation
    public int testMathThenBitwise(int a, int b) {
        int mathResult = a + b;
        return mathResult & 0x0F;                // BUG
    }

    // TP2: Using a bitwise result in an arithmetic operation
    public int testBitwiseThenMath(int flags) {
        int bitResult = flags & 0xFF;
        return bitResult + 5;                    // BUG
    }

    // TP3: Performing a bitwise shift on an arithmetic result
    public int testShiftOnMath(int a, int b) {
        int diff = a - b;
        return diff << 2;                        // BUG
    }

    // TP4: Unary minus (arithmetic negation) on a bitwise result
    public int testUnaryMinusOnBitwise(int flags) {
        int bitData = flags | 0x01;
        return -bitData;                         // BUG
    }

    // TP5: Using increment (IINC) on a bitwise result
    public int testIincOnBitwise(int flags) {
        int bitData = flags & 0x0F;
        bitData++;                               // BUG
        return bitData;
    }

    // TP6: Type tracking across primitive casts
    public int testCastBetweenOperations(int a, int b) {
        int mathResult = a + b;
        byte casted = (byte) mathResult;
        return casted & 0xFF;                    // BUG
    }

    // TP7: Control flow merge retaining the tag
    public int testMergePaths(boolean condition, int a, int b) {
        int mixedVar;
        if (condition) {
            mixedVar = a + 10;
        } else {
            mixedVar = a - b;
        }
        return mixedVar << 5;                    // BUG
    }

    // TP8: Non-compliant example 1 from NUM01-J description
    public int testNoncompliant1(int x) {
        x += (x << 2) + 1; // BUG
        return x;
    }

    // TP9: Non-compliant example 2 from NUM01-J description
    int testNoncompliant2(int x) {
        int y = x << 2;
        x += y + 1; // BUG
        return x;
    }

    // TP10: Non-compliant example 5 from NUM01-J description
    int testNoncompliant5() {
        byte[] b = new byte[]{-1, -1, -1, -1};
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = ((result << 8) + b[i]);
        }
        return result;
    }

    // TP11: Non-compliant example 5 from NUM01-J description
    int testNoncompliant6() {
        byte[] b = new byte[] {-1, -1, -1, -1};
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = ((result << 8) + (b[i] & 0xff));
        }
        return result;
    }

    // =========================================================================
    // NEGATIVE TEST CASES (COMPLIANT - Should not report a bug)
    // =========================================================================

    // TN1: Pure arithmetic operations
    public int testPureMath(int a, int b) {
        int sum = a + b;
        int diff = a - b;
        return sum * diff;                       // OK
    }

    // TN2: Pure bitwise operations
    public int testPureBitwise(int flags, int mask) {
        int masked = flags & mask;
        return masked ^ 0xFFFF;                  // OK
    }

    // TN3: Bitwise NOT
    public int testBitwiseNot(int flags) {
        int bitData = flags & 0x0F;
        return ~bitData;                         // OK
    }

    // TN4: Shift asymmetry
    public int testShiftAmountIsMath(int flags, int offset) {
        int bitData = flags & 0xFF;
        int mathOffset = offset + 2;
        return bitData << mathOffset;            // OK
    }

    // TN5: Variable reuse
    public int testVariableReuse(int a, int flags) {
        int temp = a + 5;
        System.out.println(temp);

        temp = flags & 0x0F;
        return temp | 0x01;                      // OK
    }

    // TN6: Compiler constant folding
    public int testLiteralsOnly() {
        return (5 + 3) & 0xFF;                   // OK
    }

    // TN7: Slot reuse for IINC (Loop counter reusing a bitwise slot)
    public int testSlotReuseForIINC(int flags, int baseVal) {
        int mathResult = baseVal + 10;
        {
            int tempBitwise = flags & 0x0F;
            if (tempBitwise == 0) return -1;
        }
        for (int i = 0; i < 10; i++) {
            mathResult += i;
        }
        return mathResult;                       // OK
    }

    // TN8: Cast on untagged value
    public int testUntaggedCast(long rawValue) {
        int casted = (int) rawValue;
        return casted & 0xFF;
    }

    // TN9: Compliant example 1 from NUM01-J description
    public int testCompliant1(int x) {
        return 5 * x + 1;
    }

    // TN10: Compliant example 3 from NUM01-J description
    public int testCompliant3() {
        byte[] b = new byte[] {-1, -1, -1, -1};
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = ((result << 8) | (b[i] & 0xff));
        }
        return result;
    }
}
