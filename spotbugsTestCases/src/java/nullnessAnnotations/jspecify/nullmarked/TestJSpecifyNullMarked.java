package nullnessAnnotations.jspecify.nullmarked;

import org.jspecify.annotations.NullMarked;


public class TestJSpecifyNullMarked {

    void f(Object o) {
    }

    void bar() {
        f(null);
    }
}
