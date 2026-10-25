package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

import org.jspecify.annotations.Nullable;

public class Ideas_2012_11_28<T> {

    public void doit(@Nullable T t) {

    }

    static class Subclass extends Ideas_2012_11_28<String> {

        @Override
        @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        public void doit(String s) {
            System.out.println(s.hashCode());
        }
    }


}
