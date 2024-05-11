package synchronizationLocks.privateFinalLocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BadMethodSynchronizationWithClassExposingMixedGenericOfItself {

    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static Map<String, List<Map<String, BadMethodSynchronizationWithClassExposingMixedGenericOfItself>>> getNestedMap() {
        return Collections.singletonMap("key", Collections.singletonList(Collections.singletonMap("key", new BadMethodSynchronizationWithClassExposingMixedGenericOfItself())));
    }
}
