package privateFinalLocks;

/**
 * @note There are other test cases with private accessible lock left out from here,
*        because they are already present in static or volatile test cases.
 */
public class BadSynchronizationWithAccessiblePrivateLock {
    private Object baseLock = new Object();

    public void doStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor in a descendant, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateBaseLock() {
        baseLock = new Object();
    }
}
