package ghIssues.issue4138;

/**
 * Synchronizing on a field and then null-checking the same field, in normal and
 * Yoda ("null == field") guard styles. Both must trigger
 * NP_SYNC_AND_NULL_CHECK_FIELD.
 *
 * Companion to <a href="https://github.com/spotbugs/spotbugs/issues/4138">#4138</a> (same
 * class of false negative, different detector).
 */
public class SyncNullCheckYoda {
    private static Object normal = new Object();
    private static Object yoda = new Object();

    // field == null
    public static void useNormal() {
        synchronized (normal) {
            if (normal == null) {
                throw new IllegalStateException();
            }
            normal.hashCode();
        }
    }

    // null == field (Yoda)
    public static void useYoda() {
        synchronized (yoda) {
            if (null == yoda) {
                throw new IllegalStateException();
            }
            yoda.hashCode();
        }
    }
}