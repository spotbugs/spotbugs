package ghIssues.issue4138;

/**
 * Double-checked locking on a non-volatile field, in normal and Yoda
 * ("null == field") guard styles. Both must trigger DC_DOUBLECHECK.
 *
 * Companion to <a href="https://github.com/spotbugs/spotbugs/issues/4138">#4138</a> (same
 * class of false negative, different detector).
 */
public class DoubleCheckYoda {
    private Object normal;
    private Object yoda;

    // field == null
    public Object getNormal() {
        if (normal == null) {
            synchronized (this) {
                if (normal == null) {
                    normal = new Object();
                }
            }
        }
        return normal;
    }

    // null == field (Yoda)
    public Object getYoda() {
        if (null == yoda) {
            synchronized (this) {
                if (null == yoda) {
                    yoda = new Object();
                }
            }
        }
        return yoda;
    }
}