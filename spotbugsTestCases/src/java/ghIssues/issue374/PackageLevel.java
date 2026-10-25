package ghIssues.issue374;

import org.jspecify.annotations.Nullable;

public class PackageLevel {
    public String method() {
        return methodNullable(null);
    }

    private String methodNullable(final @Nullable String test) {
        return methodNonNull(test);
    }

    private String methodNonNull(final String test) {
        return test;
    }
}
