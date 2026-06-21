package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3668">#3668</a>.
 */
public class Issue3668 {
    volatile String field = Issue3668.class.getCanonicalName();

    @ExpectWarning("NN_NAKED_NOTIFY")
    public void foo() {
        String str = "str";
        synchronized (field) {
            if (str != null) {
                field.notifyAll();
            }
        }
    }

    public void fooWithMutation() {
        synchronized (field) {
            field = "mutated";
            field.notifyAll();
        }
    }
}
