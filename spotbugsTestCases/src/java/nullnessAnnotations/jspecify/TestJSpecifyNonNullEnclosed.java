package nullnessAnnotations.jspecify;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestJSpecifyNonNullEnclosed {

    void f(Object o) {
    }

    void bar() {
        f(new Object());
        f(null);
    }
}
