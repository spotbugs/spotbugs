package nullnessAnnotations.jspecify;

import org.jspecify.annotations.NonNull;

public class TestJSpecifyNonNull {

    Object f(@NonNull Object o) {
        return o;
    }

    Object bar() {
        f(new Object());
        return f(null);
    }
}
