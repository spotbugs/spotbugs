package privateFinalLocks;

public class BadSynchronizationWithPublicNonFinalLock {
    public Object lock = new Object();

    public void changeValue() {
        synchronized (lock) { // synchronizing on publicly accessible lock object, bug should be detected here
            doSomeStuff();
        }
    }

    public void doSomeStuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}
