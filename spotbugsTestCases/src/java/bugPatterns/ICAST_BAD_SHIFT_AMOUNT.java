package bugPatterns;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class ICAST_BAD_SHIFT_AMOUNT {

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    int bug32(int any) {
        return any >> 32;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    int bug40(int any) {
        return any >> 40;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    int bug48(int any) {
        return any >> 48;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    int bug56(int any) {
        return any >> 56;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    int notBug8(int any) {
        return any >> 8;
    }

    @NoWarning("ICAST_BAD_SHIFT_AMOUNT")
    int notBug31(int any) {
        return any >> 31;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    int bug2(int any) {
        return any << 32;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    long highPriorityBug(int any) {
        return any << 32;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    long highPriorityBug(int any1, int any2) {
        return (any1 << 32) | any2;
    }

    @ExpectWarning("ICAST_BAD_SHIFT_AMOUNT")
    long highPriorityBug2(int any1, int any2) {
        return any2 | (any1 << 32);
    }
}
