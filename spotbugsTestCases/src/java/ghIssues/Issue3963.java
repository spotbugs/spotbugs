package ghIssues;

import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3963">#3963</a>.
 */
public class Issue3963 {
    @NoWarning("IL_INFINITE_LOOP")
    public void testLoop1() {
        String foo = "a";
        while (foo.length() > 2) {
        }
    }

    public void testLoop2() {
        String foo = "a";
        while (foo.length() > 2.0) {
        }
    }
}
