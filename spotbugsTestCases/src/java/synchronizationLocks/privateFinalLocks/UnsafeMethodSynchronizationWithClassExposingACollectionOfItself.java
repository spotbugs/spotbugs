package synchronizationLocks.privateFinalLocks;

import java.util.Collection;
import java.util.Collections;

public class UnsafeMethodSynchronizationWithClassExposingACollectionOfItself {
    public synchronized void doStuff() {  // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Collection<UnsafeMethodSynchronizationWithClassExposingACollectionOfItself> getCollection() {
        return Collections.singletonList(new UnsafeMethodSynchronizationWithClassExposingACollectionOfItself());
    }
}
