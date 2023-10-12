package instanceLockOnSharedStaticData.LCK00;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressFBWarnings("SWL_SLEEP_WITH_LOCK_HELD")
public class BadMethodSynchronizationLock {
    public static void unTrustedCode() throws InterruptedException {
        // Untrusted code
        String name = "untrusted";
        ClassExposingItSelf someObject = ClassExposingItSelf.lookup(name);
        synchronized (someObject) {
            while (true) {
                // Indefinitely lock someObject
                // In some time somObject.changeValue will be called that tries to lock on 'itself'(intrinsic lock)
                // but because of the above synchronized block it won't acquire the lock
                // resulting in a dead lock
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }

    public static void unTrustedCode2() throws InterruptedException {
        // Similar case to the above one, but using a Collection
        Collection<ClassExposingACollectionOfItself> collectionOfItselves = ClassExposingACollectionOfItself.getCollection();

        synchronized (collectionOfItselves.iterator().next()) {
            while (true) {
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }
}


class ClassExposingItSelf {
    // Locks on the object's monitor
    public synchronized void changeValue() {
        System.out.println("Change some value");
    }

    public static ClassExposingItSelf lookup(String name) { // exposing the lock object, bug should be detected here
        return null;
    }
}

class ClassExposingACollectionOfItself {
    public synchronized void doStuff() {
        System.out.println("Do some stuff");
    }

    public static Collection<ClassExposingACollectionOfItself> getCollection() {
        return Collections.singletonList(new ClassExposingACollectionOfItself());
    }
}

