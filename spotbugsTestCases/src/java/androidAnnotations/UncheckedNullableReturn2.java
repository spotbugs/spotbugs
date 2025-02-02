package androidAnnotations;

import androidx.annotation.Nullable;

public class UncheckedNullableReturn2 {
    @Nullable
    String foo() {
        return null;
    }

    void bar() {
        System.out.println(foo().hashCode());
    }
}
