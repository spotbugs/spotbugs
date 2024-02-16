package privateFinalLocks;

    public class BadSynchronizationWithExposedLockInParent {
    protected Object lock1 = new Object();
    protected final Object lock2 = new Object();
    protected static Object lock3 = new Object();
    protected volatile Object lock4 = new Object();
    protected Object lock5 = new Object();

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

class BadSynchronizationWithExposedLockFromParent extends BadSynchronizationWithExposedLockInParent {
    public void doSomeStuff1() {
        synchronized (lock1) {
            System.out.println("Do stuff");
        }
    }

    public void doSomeStuff2() {
        synchronized (lock2) {
            System.out.println("Do stuff");
        }
    }

    public void doSomeStuff3() {
        synchronized (lock3) {
            System.out.println("Do stuff");
        }
    }

    public void doSomeStuff4() {
        synchronized (lock4) {
            System.out.println("Do stuff");
        }
    }

    public void doSomeStuff5() {
        synchronized (lock5) {
            System.out.println("Do stuff");
        }
    }
}
