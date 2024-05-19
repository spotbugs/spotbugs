package privateFinalLocks;

import java.util.Collections;
import java.util.Map;

public class UnsafeMethodSynchronizationWithClassExposingAMapOfItself {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Map<Object, UnsafeMethodSynchronizationWithClassExposingAMapOfItself> getMap() {
        return Collections.singletonMap(new Object(), new UnsafeMethodSynchronizationWithClassExposingAMapOfItself());
    }
}