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
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }
}


class SomeClass {
    // Locks on the object's monitor
    public synchronized void changeValue() {
        // ...
    }

    public static SomeClass lookup(String name) {
        // ...
        return null;
    }
}

