package sfBugs.bug3466780;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Foo {
    public int number = 22;

    public final class Inner {
        @NoWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Inner() {
            Foo.this.number++;
        }

        @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public int foo(Object x) {
            return x.hashCode();
        }
    }

    public final class Inner2 {
        @NoWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Inner2(Object x) {
            Foo.this.number++;
        }

    }
}
