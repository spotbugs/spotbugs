package privateFinalLocks;

public class BadSynchronizationWithExposedLockInDescendant {
    protected Object lock1 = new Object();
    protected final Object lock2 = new Object();
    protected static Object lock3 = new Object();
    protected volatile Object lock4 = new Object();
    protected Object lock5 = new Object();

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

class BadSynchronizationWithExposingLockInDescendant extends BadSynchronizationWithExposedLockInDescendant {
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
}