package privateFinalLocks;

public class BadSynchronizationWithExposedLockToUntrustedCode {
    protected Object baseLock = new Object();
    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor in a descendant, bug should be detected here
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithExposedLockToUntrustedCodeFromParent extends BadSynchronizationWithExposedLockToUntrustedCode { // this is the untrusted code part
    public void updateBaseLock() {
        baseLock = new Object();
    }
}
