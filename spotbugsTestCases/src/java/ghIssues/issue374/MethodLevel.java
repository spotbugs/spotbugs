package ghIssues.issue374;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

public class MethodLevel {
    public String method() {
        return methodNullable(null);
    }

    private String methodNullable(@Nullable final String test) {
        return methodNonNull(test);
    }

    @ParametersAreNonnullByDefault
    private String methodNonNull(final String test) {
        return test;
    }
}
