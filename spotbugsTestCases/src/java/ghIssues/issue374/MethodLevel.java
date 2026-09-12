package ghIssues.issue374;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jspecify.annotations.Nullable;

public class MethodLevel {
    public String method() {
        return methodNullable(null);
    }

    private String methodNullable(final @Nullable String test) {
        return methodNonNull(test);
    }

    @ParametersAreNonnullByDefault
    private String methodNonNull(final String test) {
        return test;
    }
}
