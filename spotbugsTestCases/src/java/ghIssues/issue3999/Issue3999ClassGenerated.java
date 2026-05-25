package ghIssues.issue3999;

import jsr305.Strict;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3999">#3999</a>.
 */
@Generated
public class Issue3999ClassGenerated {
    @Strict
    Object f;

    public void violation(Object unknown) {
        f = unknown;
    }
}
