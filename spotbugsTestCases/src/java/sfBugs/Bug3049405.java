package sfBugs;

import com.github.spotbugs.jsr305.annotation.CheckForNull;
import com.github.spotbugs.jsr305.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3049405 {
    @CheckForNull
    final Object o = new Object();

    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    public void foo(@Nonnull Object o) {
        this.o.toString();
    }
}
