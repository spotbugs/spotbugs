package sfBugs;

import com.github.spotbugs.jsr305.annotation.CheckForNull;
import com.github.spotbugs.jsr305.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3567801 {

    interface Test {
        int foo(@CheckForNull Object x);
    }

    static class Impl implements Test {

        @Override
		@NoWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public int foo(@Nonnull Object x) {
            return x.hashCode();
        }

        public int bar() {
            return foo("abc");
        }
    }

}
