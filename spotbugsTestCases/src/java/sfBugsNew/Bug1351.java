package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Bug1351 {

    // FindBugs flags NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE
    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    static StringPair shouldFlagAndDoes1(@Nullable String a, @Nullable String b) {
        return new StringPair(a, b);
    }

    // FindBugs flags NP_NULL_PARAM_DEREF
    @ExpectWarning("NP_NULL_PARAM_DEREF")
    static @Nullable StringPair shouldFlagAndDoes2(@Nullable String a, @Nullable String b) {
        if (a == null && b == null) {
            return null;
        }

        return new StringPair(a, b);
    }

    // no FindBugs flagged
    static @Nullable StringPair shouldFlagButDoesnt1(@Nullable String a, @Nullable String b) {
        if (a == null) {
            return null;
        }

        return new StringPair(a, b);
    }

    // no FindBugs flagged
    static @Nullable StringPair shouldFlagButDoesnt2(@Nullable String a, @Nullable String b) {
        if (b == null) {
            return null;
        }

        return new StringPair(a, b);
    }

    static final class StringPair {

        private final String a;
        private final String b;

        StringPair(@NonNull String a, @NonNull String b) {
            this.a = a;
            this.b = b;
        }

        String getA() {
            return a;
        }

        String getB() {
            return b;
        }
    }

    private Bug1351() {

    }

}
