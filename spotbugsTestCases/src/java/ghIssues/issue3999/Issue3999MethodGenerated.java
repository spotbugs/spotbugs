package ghIssues.issue3999;

import jsr305.Strict;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3999">#3999</a>.
 */
public class Issue3999MethodGenerated {
    @Strict
    Object f;

    @Generated
    public void violation(Object unknown) {
        f = unknown;
    }
}
