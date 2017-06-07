package androidAnnotations;

import android.support.annotation.NonNull;

public class NullForNonNullParam {
    static void foo(@NonNull Object o) {
    }

    static void bar() {
        foo(null);
    }
}
