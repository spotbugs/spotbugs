package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_07_13 {

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask() {
        int numBits = 32;
        int mask = numBits == 32 ? 0xffffffff : ((1 << numBits) - 1);
        return mask;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask2() {
        int numBits = 32;
        int mask = numBits >= 32 ? 0xffffffff : ((1 << numBits) - 1);
        return mask;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask3() {
        int numBits = 32;
        int mask = numBits > 31 ? 0xffffffff : ((1 << numBits) - 1);
        return mask;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask4() {
        int numBits = 32;
        int mask = numBits != 32 ? ((1 << numBits) - 1) : 0xffffffff;
        return mask;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask5() {
        int numBits = 32;
        int mask = numBits <= 31 ? ((1 << numBits) - 1) : 0xffffffff;
        return mask;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask6() {
        int numBits = 32;
        int mask = numBits < 32 ? ((1 << numBits) - 1) : 0xffffffff;
        return mask;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMaskTP() {
        int numBits = 32;
        int mask = numBits == 31 ? 0xffffffff : ((1 << numBits) - 1);
        return mask;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask2TP() {
        int numBits = 32;
        int mask = numBits >= 33 ? 0xffffffff : ((1 << numBits) - 1);
        return mask;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask3TP() {
        int numBits = 32;
        int mask = numBits > 32 ? 0xffffffff : ((1 << numBits) - 1);
        return mask;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask4TP() {
        int numBits = 32;
        int mask = numBits != 33 ? ((1 << numBits) - 1) : 0xffffffff;
        return mask;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask5TP() {
        int numBits = 32;
        int mask = numBits <= 32 ? ((1 << numBits) - 1) : 0xffffffff;
        return mask;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    static int getMask6TP() {
        int numBits = 32;
        int mask = numBits < 33 ? ((1 << numBits) - 1) : 0xffffffff;
        return mask;
    }

}
