package bugPatterns;

public class ICAST_INTEGER_MULTIPLY_CAST_TO_LONG {

    void bug(int any1, int any2) {
        long x = any1 * any2;
    }

    void notBug(int any1) {
        long x = any1 * 1000;
    }
}
