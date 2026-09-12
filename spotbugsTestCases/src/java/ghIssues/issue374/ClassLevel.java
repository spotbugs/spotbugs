package ghIssues.issue374;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
public class ClassLevel {
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
