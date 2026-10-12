package sfBugs;

import org.jspecify.annotations.NonNull;

public class Bug1968650 {
    public void nonNullArgsMethod(@NonNull Object any) {

    }

    public @NonNull Object nonNullReturnValue() {
        nonNullArgsMethod(null);
        return null;
    }
}
