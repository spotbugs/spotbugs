package sfBugs;

import com.github.spotbugs.jsr305.annotation.Nullable;

public class Bug1965452b {

    static int foo(@Nullable Object x) {
        return x.hashCode();
    }

}
