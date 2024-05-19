package privateFinalLocks;

public class UnsafeSynchronizationWithLockExposedInDeclaringClass {
    // Protected locks
    protected Object lock1 = new Object();
    protected final Object lock2 = new Object();
    protected static Object lock3 = new Object();
    protected volatile Object lock4 = new Object();
    protected Object lock5 = new Object();

    // Private locks
    private Object lock6 = new Object();
    // private final Object lock7 = new Object(); // its fine
    private Object lock7 = new Object();
    private static Object lock8 = new Object();
    private volatile Object lock9 = new Object();

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

    public void doStuff6() {
        synchronized (lock6) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff7() {
        synchronized (lock7) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff8() {
        synchronized (lock8) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff9() {
        synchronized (lock9) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }



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
    
    public Object getLock6() {
        return lock6;
    }
    
    public void updateLock7(Object newLock) {
        lock7 = newLock;
    }
    
    public void updateLock8() {
        lock8 = new Object();
    }

    public void updateLock9() {
        Object newLock = new Object();
        lock9 = newLock;
    }
}
