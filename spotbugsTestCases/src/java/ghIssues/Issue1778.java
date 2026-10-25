package ghIssues;

import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public class Issue1778 {

    public static @Nullable Function<String, String> get() {
        return k -> "nope";
    }

    public static String getMapping(String value) {
        Objects.requireNonNull(value);

        Function<String, String> f = get();
        Objects.requireNonNull(f);

        return f.apply(value);
    }
}
