package bugPatterns;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class ICAST_INTEGER_MULTIPLY_CAST_TO_LONG {

    @ExpectWarning("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    void bug(int any1, int any2) {
        long x = any1 * any2;
        System.out.println(x);
    }

    @DesireNoWarning("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    void notBug(int any1) {
        long x = any1 * 1000;
        System.out.println(x);
    }
}
