package nullnessAnnotations.jspecify.nullmarked;

public class TestJSpecifyNullMarked {

    void f(Object o) {
    }

    void bar() {
        f(null);
    }
}
