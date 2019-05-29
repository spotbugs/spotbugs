package type.annotation;

import org.checkerframework.checker.nullness.qual.NonNull;

public class CheckerFrameworkTypeAnnotations {

    // Expecting NP_NONNULL_RETURN_VIOLATION to be thrown here
    @NonNull
    public String foo() {
        return null;
    }

}
