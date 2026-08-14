package ghIssues.issue4138;

/**
 * Spin loop on a field, in normal and Yoda ("null == field") guard styles.
 * Both must trigger SP_SPIN_ON_FIELD.
 *
 * Companion to <a href="https://github.com/spotbugs/spotbugs/issues/4138">#4138</a> (same
 * class of false negative, different detector).
 */
public class SpinLoopYoda {
    private Object normal;
    private Object yoda;

    // field == null
    public void spinNormal() {
        while (normal == null) {
            // spin
        }
    }

    // null == field (Yoda)
    public void spinYoda() {
        while (null == yoda) {
            // spin
        }
    }
}
