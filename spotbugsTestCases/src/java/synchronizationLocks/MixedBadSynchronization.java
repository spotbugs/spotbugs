package synchronizationLocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MixedBadSynchronization {
    public Map<Object, Object> collection1 = Collections.synchronizedMap(new HashMap<>());
    public final Set<Object> view1 = collection1.keySet();
    protected final Set<Map.Entry<Object, Object>> view2 = collection1.entrySet();
    final Collection<Object> view3 = collection1.values();


    protected final Set<Object> collection2 = new HashSet<>();
    protected final Set<Object> view4 = collection2;


    private static List<Object> collection3 = Collections.synchronizedList(Arrays.asList(new Object(), new Object(),
            new Object()));
    protected final List<Object> view5 = collection3.subList(0, 1);


    private volatile Collection<Object> collection4 = new ArrayList<>();
    private final Collection<Object> view6 = collection4;


    // Exposing functions
    public List<Object> getCollection3() {
        return collection3;
    }

    public List<Object> getView5() {
        return view5;
    }

    public Collection<Object> getCollection4() {
        return collection4;
    }

    public Collection<Object> getView6() {
        return view6;
    }


    // Synchronizations on views
    public void doStuff1() {
        synchronized (view1) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff2() {
        synchronized (view2) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff3() {
        synchronized (view3) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff4() {
        synchronized (view4) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff6() {
        synchronized (view6) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

}

class MixedSynchronizationFromParent extends MixedBadSynchronization {
    // Exposure of backing collections
    public Set<Object> getCollection2() {
        return collection2;
    }

    public Set<Object> getView4() {
        return view4;
    }


    // Synchronizations on views
    public void doStuff5() {
        synchronized (view5) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }
}
