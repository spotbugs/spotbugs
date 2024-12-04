package synchronizationLocks.privateFinalLocks;

public class SafeMethodSynchronization {
    public synchronized void doStuff() {
        System.out.println("Do some stuff");
    }

    private SafeMethodSynchronization factory() { // this should only be fine if the constructors were private
        return new SafeMethodSynchronization();
    }

    private static final Object lock = new Object();
    public void doOtherStuff() {
        synchronized (lock) {
            System.out.println("Do some other stuff");
        }
    }
}
