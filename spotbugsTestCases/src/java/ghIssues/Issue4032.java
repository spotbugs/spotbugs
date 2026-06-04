package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4032">#4032</a>.
 */
public class Issue4032 {

    private static final String[] names = new String[] { "a", "b" };

    public interface Provider {
        String[] getNames();
    }

    public static class NamedProvider implements Provider {
        @Override
        @ExpectWarning("EI_EXPOSE_REP")
        public String[] getNames() {
            return names;
        }
    }

    public static Provider anonymousProvider() {
        return new Provider() {
            @Override
            @ExpectWarning("EI_EXPOSE_REP")
            public String[] getNames() {
                return names;
            }
        };
    }

    @NoWarning("EI_EXPOSE_REP")
    public static String[] getNamesCopy() {
        return names.clone();
    }
}
