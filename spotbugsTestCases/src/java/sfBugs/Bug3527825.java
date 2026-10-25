package sfBugs;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bug3527825 {
    public interface GenericFindBugsParameterChecking<T> {
        public void doSomething(@Nullable T theParameter);
    }

    public static class GenericFindBugsParameterCheckingImpl implements GenericFindBugsParameterChecking<String> {
        @Override
        public void doSomething(@NonNull String theParameter) {
            // do something
        }
    }

    public interface FindBugsParameterChecking {
        public void doSomething(@Nullable String theParameter);
    }

    public static class FindBugsParameterCheckingImpl implements FindBugsParameterChecking {
        @Override
        public void doSomething(@NonNull String theParameter) {
            // do something
        }
    }
}
