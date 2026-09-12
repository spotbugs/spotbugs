package sfBugs;

import org.jspecify.annotations.Nullable;

public class Bug1828288 {

    public @Nullable Object field;

    @Override
    public String toString() {
        return field.toString(); // (*)
    }

}
