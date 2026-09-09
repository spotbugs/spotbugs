package ghIssues;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4032">#4032</a>.
 *
 * <p>EI_EXPOSE_REP must be reported identically for a public getter that returns an internal
 * mutable array regardless of whether the getter lives in a named nested class or in an
 * anonymous class implementing the same public interface.
 */
public class Issue4032 {
    private static final String[] names = new String[] {"a", "b"};

    public interface Provider {
        String[] getNames();
    }

    public static class NamedProvider implements Provider {
        @Override
        public String[] getNames() {
            return names;
        }
    }

    public static Provider anonymousProvider() {
        return new Provider() {
            @Override
            public String[] getNames() {
                return names;
            }
        };
    }
}
