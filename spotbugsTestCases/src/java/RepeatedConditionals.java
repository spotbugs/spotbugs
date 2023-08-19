import java.io.File;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class RepeatedConditionals {
    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean simple(int a, int b) {
        return a == b && a == b;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean unboxing(Integer a) {
        return a > 5 && a > 5;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean strings(String a, String b) {
        return a.trim().compareTo(b.trim()) > 0 && a.trim().compareTo(b.trim()) > 0;
    }

    @ExpectWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean equalsTest(File a, File b) {
        return a.equals(b) && a.equals(b);
    }

    @NoWarning("RpC_REPEATED_CONDITIONAL_TEST")
    public boolean sideEffect(File a) {
        return a.delete() && a.delete();
    }
}
