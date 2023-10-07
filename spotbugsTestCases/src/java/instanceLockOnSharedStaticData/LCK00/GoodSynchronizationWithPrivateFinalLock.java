package instanceLockOnSharedStaticData.LCK00;

public class GoodSynchronizationWithPrivateFinalLock {
    private final Object lock = new Object(); // private final lock object
    public void changeValue() {
        synchronized (lock) { // Locks on the private Object
            System.out.println("Change some values");
        }
    }
}
