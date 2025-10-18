package nullnessAnnotations;

import org.jspecify.annotations.Nullable;

public class UncheckedJSpecifyNullableReturn {
    @Nullable
    String foo() {
        return null;
    }

    void bar() {
        System.out.println(foo().hashCode());
    }
}
