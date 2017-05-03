package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class TestFieldAnnotations {
    @NonNull
    Object x;

    @CheckForNull
    Object y;

    void bug0() {
        x = y;
    }

    void fp1() {
        y = x;
    }

    void dodgy0() {
        if (x == null)
            System.out.println("Huh?");
    }

    void fp2() {
        if (y != null) {
            System.out.println(y.hashCode());
        }
    }

    void noop() {
    }

    int fp3() {
        if (y == null)
            return 0;
        return y.hashCode();
    }

    int fp4() {
        if (y == null)
            return 0;
        noop();
        return y.hashCode();
    }

}
