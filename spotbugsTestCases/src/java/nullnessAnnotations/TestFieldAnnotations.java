package nullnessAnnotations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestFieldAnnotations {
    @NonNull
    Object x;

    @Nullable
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
