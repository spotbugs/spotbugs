package sfBugsNew;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public final class Bug1351 {

    // FindBugs flags NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE
    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    static StringPair shouldFlagAndDoes1(@CheckForNull String a, @CheckForNull String b) {
        return new StringPair(a, b);
    }

    // FindBugs flags NP_NULL_PARAM_DEREF
    @ExpectWarning("NP_NULL_PARAM_DEREF")
    @CheckForNull
    static StringPair shouldFlagAndDoes2(@CheckForNull String a, @CheckForNull String b) {
        if (a == null && b == null) {
            return null;
        }

        return new StringPair(a, b);
    }

    // no FindBugs flagged
    @CheckForNull
    static StringPair shouldFlagButDoesnt1(@CheckForNull String a, @CheckForNull String b) {
        if (a == null) {
            return null;
        }

        return new StringPair(a, b);
    }

    // no FindBugs flagged
    @CheckForNull
    static StringPair shouldFlagButDoesnt2(@CheckForNull String a, @CheckForNull String b) {
        if (b == null) {
            return null;
        }

        return new StringPair(a, b);
    }

    static final class StringPair {

        private final String a;
        private final String b;

        StringPair(@Nonnull String a, @Nonnull String b) {
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
