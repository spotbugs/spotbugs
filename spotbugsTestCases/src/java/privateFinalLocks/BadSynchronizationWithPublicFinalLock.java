package privateFinalLocks;

public class BadSynchronizationWithPublicFinalLock {
    public final Object lock = new Object();

    public void changeValue() {
        synchronized (lock) { // synchronizing on the public lock object, bug should be detected here
            doSomeStuff();
        }
    }

    public void doSomeStuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}

