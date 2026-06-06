package nullnessAnnotations.jspecify;

import org.jspecify.annotations.NonNull;

@NonNull
public class TestJSpecifyNonNullEnclosed {

    void f(Object o) {
    }

    void bar() {
        f(new Object());
        f(null);
    }
}
