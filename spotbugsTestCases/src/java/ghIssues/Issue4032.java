package ghIssues;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4032">#4032</a>.
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
