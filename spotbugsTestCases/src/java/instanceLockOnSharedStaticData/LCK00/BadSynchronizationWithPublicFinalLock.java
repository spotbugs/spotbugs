package instanceLockOnSharedStaticData.LCK00;

public class BadSynchronizationWithPublicFinalLock {
    public final Object lock = new Object();

    public void changeValue() {
        synchronized (lock) {
            doSomeStuff();
        }
    }

    public void doSomeStuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}

