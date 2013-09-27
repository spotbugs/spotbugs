import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class SelfFieldOperation {
    int x, y;

    volatile int z;

    boolean volatileFalsePositive() {
        return z == z;
    }

    int f() {
        if (x < x)
            x = y ^ y;
        if (x != x)
            y = x | x;
        if (x >= x)
            x = y & y;
        if (y > y)
            y = x - x;
        return x;
    }

    Integer a, b;

    @ExpectWarning("SA_FIELD_SELF_COMPARISON")
    boolean e() {
        return a.equals(a);
    }

    @ExpectWarning("SA_FIELD_SELF_COMPARISON")
    int c() {
        return a.compareTo(a);
    }

}
