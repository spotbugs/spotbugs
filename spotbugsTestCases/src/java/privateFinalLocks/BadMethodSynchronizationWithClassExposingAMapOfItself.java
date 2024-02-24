package privateFinalLocks;

import java.util.Collections;
import java.util.Map;

public class BadMethodSynchronizationWithClassExposingAMapOfItself {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Map<Object, BadMethodSynchronizationWithClassExposingAMapOfItself> getMap() {
        return Collections.singletonMap(new Object(), new BadMethodSynchronizationWithClassExposingAMapOfItself());
    }
}