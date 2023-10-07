package instanceLockOnSharedStaticData.LCK00;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("SWL_SLEEP_WITH_LOCK_HELD")
public class BadMethodSynchronizationLock {
    public static void unTrustedCode() throws InterruptedException {
        // Untrusted code
        String name = "untrusted";
        SomeClass someObject = SomeClass.lookup(name);
        // if (someObject == null) {
        //            // ... handle error
        //        }
        synchronized (someObject) {
            while (true) {
                // Indefinitely lock someObject
                // In some time somObject. change value will be called that tries to lock on 'itself'
                // but because of the above synchronized block it won't acquire the lock
                // resulting in a dead lock
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }
}


class SomeClass {
    // Locks on the object's monitor
    public synchronized void changeValue() {
        System.out.println("Change some value");
    }

    public static SomeClass lookup(String name) { // exposing the lock object, bug should be detected here
        // ...
        return null;
    }
}

