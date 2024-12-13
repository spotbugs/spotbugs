package synchronizationLocks.privateFinalLocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself {

    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Map<String, List<Map<String, UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself>>> getNestedMap() {
        return Collections.singletonMap("key", Collections.singletonList(Collections.singletonMap("key", new UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself())));
    }
}
