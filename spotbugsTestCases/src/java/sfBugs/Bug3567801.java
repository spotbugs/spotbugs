package sfBugs;

import javax.annotation.CheckForNull;

import org.jspecify.annotations.NonNull;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3567801 {

    interface Test {
        int foo(@CheckForNull Object x);
    }

    static class Impl implements Test {

        @Override
        @NoWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public int foo(@NonNull Object x) {
            return x.hashCode();
        }

        public int bar() {
            return foo("abc");
        }
    }

}
