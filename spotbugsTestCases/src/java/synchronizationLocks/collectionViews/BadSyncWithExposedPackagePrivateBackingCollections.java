package synchronizationLocks.collectionViews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BadSyncWithExposedPackagePrivateBackingCollections {
    Map<Object, Object> collection1 = Collections.synchronizedMap(new HashMap<>());
    private final Set<Object> view1 = collection1.keySet();
    private final Set<Map.Entry<Object, Object>> view2 = collection1.entrySet();
    private final Collection<Object> view3 = collection1.values();

    final Set<Object> collection2 = new HashSet<>();
    private final Set<Object> view4 = collection2;

    static List<Object> collection3 = Collections.synchronizedList(Arrays.asList(new Object(), new Object(),
            new Object()));
    private final List<Object> view5 = collection3.subList(0, 1);

    volatile Collection<Object> collection4 = new ArrayList<>();
    private final Collection<Object> view6 = collection4;

    public void doStuff1() {
        synchronized (view1) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff2() {
        synchronized (view2) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff3() {
        synchronized (view3) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff4() {
        synchronized (view4) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff5() {
        synchronized (view5) { /* detect bug here */
            System.out.println("Do stuff");
        }
    }

}
