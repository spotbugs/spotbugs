package nullnessAnnotations;

import org.jspecify.annotations.NonNull;

public class TestNonNull5 {

    @NonNull
    Object f() {
        return null;
    }

}
