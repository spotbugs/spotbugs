package synchronizationLocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GoodSynchronization {
    private final Map<Object, Object> collection1 = Collections.synchronizedMap(new HashMap<>());
    private final Set<Object> view1 = collection1.keySet();

    public void doStuff1() {
        synchronized (collection1) {
            System.out.println("Do stuff");
        }
    }

    public void doStuff2() {
        synchronized (view1) {
            System.out.println("Do stuff");
        }
    }
}
