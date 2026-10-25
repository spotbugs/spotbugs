package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class AnnotationsOnOverridenMethods {

    @NonNull
    Object foo(@Nullable Object o) {
        return o;
    }

    Object bar(Object o) {
        return o;
    }

    static class Child extends AnnotationsOnOverridenMethods {

        @Override
        @ExpectWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION,NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
        @Nullable
        Object foo(@NonNull Object o) {
            return o;
        }

        @Override
        @Nullable
        Object bar(@NonNull Object o) {
            return o;
        }
    }
}
