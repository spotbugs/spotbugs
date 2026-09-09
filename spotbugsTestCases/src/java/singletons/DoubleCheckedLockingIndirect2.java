package singletons;

public class DoubleCheckedLockingIndirect2 {
    private static DoubleCheckedLockingIndirect2 instance;

    private DoubleCheckedLockingIndirect2()
    {
    }

    private static void outerInit() {
        init();
    }

    private static void init() {
        synchronized (DoubleCheckedLockingIndirect2.class) {
            innerInit();
        }
    }

    private static void innerInit()
    {
        if (instance == null)
            instance = new DoubleCheckedLockingIndirect2();
    }

    public static DoubleCheckedLockingIndirect2 getInstance()
    {
        if (instance == null)
            outerInit();
        return instance;
    }

}
