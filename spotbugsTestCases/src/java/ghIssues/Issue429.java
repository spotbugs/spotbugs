package ghIssues;

import static com.google.common.truth.Truth.assertThat;

/**
 * An implementation to reproduce bug, shared by <a href="https://github.com/spotbugs/spotbugs/issues/429">#429</a>.
 */
public class Issue429 {
    public void method() {
        assertThat(System.currentTimeMillis() > 0);
    }
}
