package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bug3049405 {
    final @Nullable Object o = new Object();

    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    public void foo(@NonNull Object o) {
        this.o.toString();
    }
}
