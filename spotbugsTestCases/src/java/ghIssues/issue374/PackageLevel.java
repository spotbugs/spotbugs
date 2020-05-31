package ghIssues.issue374;

import javax.annotation.Nullable;

/** A package is annotated with{@code @ParametersAreNonnullByDefault} */
public class PackageLevel {
    /** @return a String. */
    public String method() {
        return methodNullable(null);
    }

    private String methodNullable(@Nullable final String test) {
        return methodNonNull(test);
    }

    private String methodNonNull(final String test) {
        return test;
    }
}
