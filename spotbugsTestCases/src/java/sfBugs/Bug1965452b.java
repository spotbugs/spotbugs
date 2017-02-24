package sfBugs;

import javax.annotation.Nullable;

public class Bug1965452b {

    static int foo(@Nullable Object x) {
        return x.hashCode();
    }

}
