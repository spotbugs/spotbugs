package type.annotation;

import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CheckerFrameworkTypeAnnotations {

    // Expecting NP_NONNULL_RETURN_VIOLATION to be thrown here
    @NonNull
    public String returningNullOnNonNullMethod() {
        return null;
    }

    // We should NOT get a NP_NONNULL_RETURN_VIOLATION here: the list contains non null elements
    // but it can be null itself
    public <V extends @NonNull Object> List<@NonNull V> returningNullWithNonNullOnGenerics() {
        return null;
    }

    // Expecting NP_NONNULL_RETURN_VIOLATION to be thrown here
    @NonNull
    public <V extends @NonNull Object> List<@NonNull V> returningNullOnNonNullMethodWithNonNullOnGenerics() {
        return null;
    }
    
    // Expecting NP_NONNULL_PARAM_VIOLATION to be thrown here
    public void usingNullForNonNullParameter() {
    	nonNullParameter(null);
    }
    
    public void nonNullParameter(@NonNull Object o) {
    }

    // We should NOT get a NP_NONNULL_RETURN_VIOLATION here: the list contains non null elements
    // but it can be null itself
    public void usingNullParameterWithNonNullOnGenerics() {
    	parameterWithNonNullOnGenerics(null);
    }
    
    public <V extends @NonNull Object> void parameterWithNonNullOnGenerics(List<@NonNull V> list) {
    }

    // Expecting NP_NONNULL_PARAM_VIOLATION to be thrown here
    public void usingNullOnNonNullParameterWithNonNullOnGenerics() {
    	nonNullParameterWithNonNullOnGenerics(null);
    }
    
    public <V extends @NonNull Object> void nonNullParameterWithNonNullOnGenerics(@NonNull List<@NonNull V> list) {
    }
}
