package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NonNull;

public class TestNonNull1 {

    Object f(@NonNull Object o) {
        return o;
    }

    Object bar() {
        return f(null);
    }
}
