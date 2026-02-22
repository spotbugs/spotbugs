package synchronizationLocks.privateFinalLocks;

public class UnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism {
    // Public locks
    public Object lock1 = new Object();
    public final Object lock2 = new Object();
    public static Object lock3 = new Object();
    public volatile Object lock4 = new Object();
    public Object lock5 = new Object();

    // Protected locks
    protected Object lock6 = new Object();
    protected final Object lock7 = new Object();
    protected static Object lock8 = new Object();
    protected volatile Object lock9 = new Object();

    // Package-private locks
    Object lock10 = new Object();
    final Object lock11 = new Object();
    static Object lock12 = new Object();
    volatile Object lock13 = new Object();
}

class UnsafeSynchronizationWithVisibleLocksFromHierarchy extends UnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism {
    public void doStuff1() {
        synchronized (lock1) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff1Again() {
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

    public void doStuff10() {
        synchronized (lock10) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff11() {
        synchronized (lock11) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff12() {
        synchronized (lock12) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff13() {
        synchronized (lock13) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }
}
