package sfBugsNew;

import com.google.common.base.Objects;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1210 {
    @NoWarning("EC_NULL_ARG")
    public static boolean test(Object x) {
        return Objects.equal(x, null);
    }
}
