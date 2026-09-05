package ghIssues.issue4138;

/**
 * Yoda-style ("null == field") variants of the lazy initialization idiom.
 *
 * These are semantics-preserving rewrites of the guards in {@code LazyInit} and
 * must trigger LI_LAZY_INIT_STATIC exactly like the "field == null" form.
 *
 * See <a href="https://github.com/spotbugs/spotbugs/issues/4138">#4138</a>
 */
public class LazyInitYoda {
    static Object foo;

    static String[] y;

    // This should be reported
    public static Object getFoo() {
        if (null == foo)
            foo = new Object();
        return foo;
    }

    // This should be reported
    public static String[] getY() {
        if (null == y) {
            y = new String[] { "a", "b", "c", "d", "e" };
        }
        return y;
    }
}
