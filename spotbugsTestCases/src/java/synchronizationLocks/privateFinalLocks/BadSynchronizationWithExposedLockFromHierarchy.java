package synchronizationLocks.privateFinalLocks;

public class BadSynchronizationWithExposedLockFromHierarchy {
    protected Object lock1 = new Object();
    protected final Object lock2 = new Object();
    protected static Object lock3 = new Object();
    protected volatile Object lock4 = new Object();
    protected Object lock5 = new Object();

    // More complex cases
    protected Object lock6 = new Object();

    public Object getLock1() {
        return lock1;
    }

    public Object getLock2() {
        return lock2;
    }

    public void updateLock3(Object newLock) {
        lock3 = newLock;
    }

    public void updateLock4() {
        lock4 = new Object();
    }

    public void updateLock5() {
        Object newLock = new Object();
        lock5 = newLock;
    }

}

class BadSynchronizationUsingExposedLockFromHierarchy extends BadSynchronizationWithExposedLockFromHierarchy {
    public void doStuff1() {
        synchronized (lock1) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff2() {
        synchronized (lock2) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff3() {
        synchronized (lock3) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff4() {
        synchronized (lock4) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff5() {
        synchronized (lock5) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    // More complex cases

    public Object getLock6() {
        return lock6;
    }
}

// a class to test more complex cases
class BadSynchronizationUsingExposedLockFromHierarchy2 extends BadSynchronizationUsingExposedLockFromHierarchy {
    // More complex cases

    public void doStuff6() {
        synchronized (lock6) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }
}
