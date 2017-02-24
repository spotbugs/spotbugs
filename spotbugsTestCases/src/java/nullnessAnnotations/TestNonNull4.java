package nullnessAnnotations;

public class TestNonNull4 extends TestNonNull3 {

    @Override
    Object f(Object o) {
        return o;
    }

    Object baz() {
        return f(null);
    }
}
