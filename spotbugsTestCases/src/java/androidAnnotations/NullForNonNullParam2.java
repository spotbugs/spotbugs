package androidAnnotations;

import androidx.annotation.NonNull;

public class NullForNonNullParam2 {
    static void foo(@NonNull Object o) {
    }

    static void bar() {
        foo(null);
    }
}
