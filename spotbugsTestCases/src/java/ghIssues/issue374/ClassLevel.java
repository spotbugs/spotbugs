package ghIssues.issue374;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
/** A class is annotated with{@code @ParametersAreNonnullByDefault} */
@ParametersAreNonnullByDefault
public class ClassLevel {
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