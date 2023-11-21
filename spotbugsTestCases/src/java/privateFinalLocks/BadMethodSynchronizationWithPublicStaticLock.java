package privateFinalLocks;

public class BadMethodSynchronizationWithPublicStaticLock {

    public static void unTrustedCode() throws InterruptedException {
        synchronized (SomeOtherClass.class) { // preventing the synchronized changeValue to acquire the lock,
            while (true) {
                Thread.sleep(Integer.MAX_VALUE); // Indefinitely delay someObject
            }
        }
    }
}

class SomeOtherClass {
    public static synchronized void changeValue() { // locking on the class, bug should be detected here
        System.out.println("Change some value");
    }
}