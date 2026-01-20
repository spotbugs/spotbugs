package sfBugs;

import jakarta.annotation.Nullable;

public class Bug1965452b {

    static int foo(@Nullable Object x) {
        return x.hashCode();
    }

}
