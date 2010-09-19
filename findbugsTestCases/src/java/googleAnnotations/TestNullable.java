package googleAnnotations;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Nullable;

@ParametersAreNonnullByDefault
public class TestNullable {
    static void foo(@Nullable Object o) {
    }

    static void bar() {
        foo(null);
    }

    static void foo2(@edu.umd.cs.findbugs.annotations.CheckForNull Object o) {
    }

    static void bar2() {
        foo2(null);
    }
}
