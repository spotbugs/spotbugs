package singletons;

/**
 * See <a href="https://github.com/spotbugs/spotbugs/issues/2934">#2934</a>
 */
public class InnerChildInstance {
    private static class Unknown extends InnerChildInstance {
    }

    private static final InnerChildInstance UNKNOWN = new Unknown();

    public static InnerChildInstance getUnknown() {
        return UNKNOWN;
    }
}
