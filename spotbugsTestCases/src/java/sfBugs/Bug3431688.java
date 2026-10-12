package sfBugs;

import javax.annotation.CheckForNull;

import org.jspecify.annotations.NonNull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3431688 {

    public interface MyInterface {
        @NonNull
        Object foo(@CheckForNull Object o);
    }

    public static class MyImplementation implements MyInterface {
        @Override
        @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public Object foo(Object o) {
            return o;
        }
    }
}
