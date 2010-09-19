package bugPatterns;

public class ICAST_BAD_SHIFT_AMOUNT {

    int bug32(int any) {
        return any >> 32;
    }

    int bug40(int any) {
        return any >> 40;
    }

    int bug48(int any) {
        return any >> 48;
    }

    int bug56(int any) {
        return any >> 56;
    }

    int notBug8(int any) {
        return any >> 8;
    }

    int notBug31(int any) {
        return any >> 31;
    }

    int bug2(int any) {
        return any << 32;
    }

    long highPriorityBug(int any) {
        return any << 32;
    }

    long highPriorityBug(int any1, int any2) {
        return (any1 << 32) | any2;
    }

    long highPriorityBug2(int any1, int any2) {
        return any2 | (any1 << 32);
    }
}
