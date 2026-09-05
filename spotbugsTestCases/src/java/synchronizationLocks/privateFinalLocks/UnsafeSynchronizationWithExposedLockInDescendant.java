package synchronizationLocks.privateFinalLocks;

public class UnsafeSynchronizationWithExposedLockInDescendant {
    protected Object lock1 = new Object();
    protected final Object lock2 = new Object();
    protected static Object lock3 = new Object();
    protected volatile Object lock4 = new Object();
    protected Object lock5 = new Object();

    // More complex cases
    protected Object lock6 = new Object();

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
}

class UnsafeSynchronizationWithExposingLockInDescendant extends UnsafeSynchronizationWithExposedLockInDescendant {
   public Object getLock1() { /* detect bug here */
       return lock1;
   }

    public Object getLock2() { /* detect bug here */
        return lock2;
    }

   public void updateLock3(Object newLock) { /* detect bug here */
       lock3 = newLock;
   }

   public void updateLock4() { /* detect bug here */
       lock4 = new Object();
   }

   public void updateLock5() { /* detect bug here */
       Object newLock = new Object();
       lock5 = newLock;
   }

   // More complex cases

    public void doStuff6() {
        synchronized (lock6) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }
}

class UnsafeSynchronizationWithExposingLockInDescendant2 extends UnsafeSynchronizationWithExposingLockInDescendant {
    // More complex cases

    public Object getLock6() {
        return lock6;
    }
}