package nullnessAnnotations;

import org.jspecify.annotations.Nullable;

public class CheckedJakartaNullableReturn {
    @Nullable
    String foo() {
        return null;
    }

    void bar() {
        String foo = foo();
        if (foo != null) {
            System.out.println(foo.hashCode());
        }
    }
}
