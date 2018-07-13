package sfBugs;

import com.github.spotbugs.jsr305.annotation.CheckForNull;
import com.github.spotbugs.jsr305.annotation.Nonnull;

public class Bug3527825 {
    public interface GenericFindBugsParameterChecking<T> {
        public void doSomething(@CheckForNull T theParameter);
    }

    public static class GenericFindBugsParameterCheckingImpl implements GenericFindBugsParameterChecking<String> {
        @Override
        public void doSomething(@Nonnull String theParameter) {
            // do something
        }
    }

    public interface FindBugsParameterChecking {
        public void doSomething(@CheckForNull String theParameter);
    }

    public static class FindBugsParameterCheckingImpl implements FindBugsParameterChecking {
        @Override
        public void doSomething(@Nonnull String theParameter) {
            // do something
        }
    }
}
