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

public class BadSyncWithBackingCollectionExposedInPlace {
    // Protected locks
    protected Map<Object, Object> collection1 = Collections.synchronizedMap(new HashMap<>());
    private final Set<Object> view1 = collection1.keySet();
    private final Set<Map.Entry<Object, Object>> view2 = collection1.entrySet();
    private final Collection<Object> view3 = collection1.values();

    protected final Set<Object> collection2 = new HashSet<>();
    private final Set<Object> view4 = collection2;

    protected static List<Object> collection3 = Collections.synchronizedList(Arrays.asList(new Object(), new Object(), new Object()));
    private final List<Object> view5 = collection3.subList(0, 1);

    protected volatile Collection<Object> collection4 = new ArrayList<>();
    private final Collection<Object> view6 = collection4;

    private Map<Object, Object> collection5 = new HashMap<>();
    protected Map<Object, Object> view7 = Collections.synchronizedMap(collection5);
    private final Set<Object> view8 = view7.keySet();

    // Private locks
    private Map<Object, Object> collection6 = Collections.synchronizedMap(new HashMap<>());
    private final Set<Object> view9 = collection6.keySet();
    private final Set<Map.Entry<Object, Object>> view10 = collection6.entrySet();
    private final Collection<Object> view11 = collection6.values();

    private final Set<Object> collection7 = new HashSet<>();
    private final Set<Object> view12 = collection7;

    private static List<Object> collection8 = Collections.synchronizedList(Arrays.asList(new Object(), new Object(), new Object()));
    private final List<Object> view13 = collection8.subList(0, 1);

    private volatile Collection<Object> collection9 = new ArrayList<>();
    private final Collection<Object> view14 = collection9;

    private Map<Object, Object> collection10 = new HashMap<>();
    private Map<Object, Object> view15 = Collections.synchronizedMap(collection10);
    private final Set<Object> view16 = view15.keySet();

    protected Map<Object, Object> collection11 = new HashMap<>();
    private Map<Object, Object> view17 = Collections.synchronizedMap(collection11);
    private final Set<Object> view18 = view17.keySet();

    // Complex case where both backing collections are exposed
    private Map<Object, Object> collection12 = new HashMap<>();
    protected Map<Object, Object> view19 = Collections.synchronizedMap(collection12);
    private final Set<Object> view20 = view19.keySet();

    // Primitive collections - not supported
    private Integer[] collection13 = new Integer[20];
    private final List<Object> view21 = Arrays.asList(collection13);

    private boolean[] collection14 = new boolean[10];
    private final List<Object> view22 = Arrays.asList(collection14);

    // Exposing methods
    public Map<Object, Object> getCollection1() {
        return collection1;
    }

    public Set<Object> getCollection2() {
        return collection2;
    }

    public List<Object> getCollection3() {
        return collection3;
    }

    public void setCollection4(Collection<Object> collection4) {
        this.collection4 = collection4;
    }

    public Map<Object, Object> getView7() {
        return view7;
    }

    public Map<Object, Object> getCollection6() {
        return collection6;
    }

    public Set<Object> getCollection7() {
        return collection7;
    }

    public void setCollection8(List<Object> collection8) {
        this.collection8 = collection8;
    }

    public Collection<Object> getCollection9() {
        return this.collection9;
    }

    public Map<Object, Object> getView15() {
        return this.view15;
    }

    public Map<Object, Object> getCollection11() {
        return this.collection11;
    }

    public Map<Object, Object> getCollection12() {
        return this.collection12;
    }

    public Map<Object, Object> getView19() {
        return this.view19;
    }

    public Integer[] getCollection13() {
        return this.collection13;
    }

    public boolean[] getCollection14() {
        return this.collection14;
    }

    // Synchronizations
    public void doStuff1() {
        synchronized (view1) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff2() {
        synchronized (view2) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff3() {
        synchronized (view3) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff4() {
        synchronized (view4) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff5() {
        synchronized (view5) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff6() {
        synchronized (view6) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff8() {
        synchronized (view8) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff9() {
        synchronized (view9) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff10() {
        synchronized (view10) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff11() {
        synchronized (view11) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff12() {
        synchronized (view12) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff13() {
        synchronized (view13) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff14() {
        synchronized (view14) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff16() {
        synchronized (view16) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff18() {
        synchronized (view18) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff20() {
        synchronized (view20) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff21() {
        synchronized (view21) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff22() {
        synchronized (view22) { /* using view as lock with an exposed backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }
}
