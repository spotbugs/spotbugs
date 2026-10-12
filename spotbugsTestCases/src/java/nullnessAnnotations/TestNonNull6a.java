package nullnessAnnotations;

import org.jspecify.annotations.NonNull;

public class TestNonNull6a {
    public static int f(@NonNull Object o) {
        return o.hashCode();
    }

}
