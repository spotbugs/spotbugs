package singletons;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3045">GitHub issue #3045</a>
 */
public class DoubleCheckedLockingIndirect {
    private static DoubleCheckedLockingIndirect instance;

    private DoubleCheckedLockingIndirect()
    {
    }

    private static synchronized void init()
    {
        if (instance == null)
            instance = new DoubleCheckedLockingIndirect();
    }

    public static DoubleCheckedLockingIndirect getInstance()
    {
        if (instance == null)
            init();
        return instance;
    }
}
