package privateFinalLocks;

public class BadSynchronizationWithPubliclyAccessibleNonFinalLock {
    private volatile Object lock = new Object();

    public void changeValue() {
        synchronized (lock) { // synchronizing on accessible lock object, bug should be detected here
            doSomeStuff();
        }
    }

    public void doSomeStuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}
