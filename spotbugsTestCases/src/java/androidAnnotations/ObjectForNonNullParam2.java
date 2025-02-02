package androidAnnotations;

import androidx.annotation.NonNull;

public class ObjectForNonNullParam2 {
    static void foo(@NonNull Object o) {
    }

    static void bar() {
        foo(new Object());
    }
}
