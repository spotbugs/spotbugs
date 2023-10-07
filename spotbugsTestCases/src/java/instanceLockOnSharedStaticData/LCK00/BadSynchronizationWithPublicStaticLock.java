package instanceLockOnSharedStaticData.LCK00;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("SWL_SLEEP_WITH_LOCK_HELD")
public class BadSynchronizationWithPublicStaticLock {

    public static void unTrustedCode() throws InterruptedException {
        // Untrusted code
        synchronized (SomeOtherClass.class) { // locking on the class, preventing the synchronized changeValue to acquire the lock, bug should be detected here
            while (true) {
                Thread.sleep(Integer.MAX_VALUE); // Indefinitely delay someObject
            }
        }
    }
}


class SomeOtherClass {
    //changeValue locks on the class object's monitor
    public static synchronized void changeValue() {
        System.out.println("Change some value");
    }


}