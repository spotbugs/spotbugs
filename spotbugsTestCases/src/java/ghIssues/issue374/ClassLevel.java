package ghIssues.issue374;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ClassLevel {
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
