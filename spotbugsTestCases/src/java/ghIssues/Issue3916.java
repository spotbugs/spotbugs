package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3916">#3916</a>.
 */
public class Issue3916 {
    @ExpectWarning("NP")
    private void foo1(String s1) {
        if (s1 != null) {
            return;
        }
        String str = s1.toString();
    }

    @ExpectWarning("NP")
    private void foo2(String s2) {
        if (s2 instanceof String) {
            return;
        }
        String str = s2.toString();
    }
}
