package accessibleCollectionView.temp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class BadSyncWithAccessiblePublicBackingCollectionInPlace {

    // Synchronized collection views
    public ArrayList<Object> collection1 = new ArrayList<>();
    private final Collection<Object> view1 = Collections.synchronizedCollection(collection1);

    public final List<Integer> collection2 = new ArrayList<>();
    private final List<Integer> view2 = Collections.synchronizedList(collection2);

    public volatile Map<Object, Object> collection3 = new TreeMap<>();
    private final Map<Object, Object> view3 = Collections.synchronizedMap(collection3);
            //collection3.keySet();

    public SortedMap<Object, Object> collection4 = new TreeMap<>();
    private final Map<Object, Object> view4 = Collections.synchronizedSortedMap(collection4);
            //collection4.values();

    public NavigableMap<Object, Object> collection5 = new TreeMap<>();
    private final Map<Object, Object> view5 = Collections.synchronizedNavigableMap(collection5);
            //collection6.entrySet();

    public static Set<Object> collection6 = new HashSet<>();
    private final Set<Object> view6 = Collections.synchronizedSet(collection6);

    public static SortedSet<Object> collection7 = new TreeSet<>();
    private final Set<Object> view7 = Collections.synchronizedSortedSet(collection7);

    public static NavigableSet<Object> collection8 = new TreeSet<>();
    private final Set<Object> view8 = Collections.synchronizedNavigableSet(collection8);


    // unmodifiable collections
    public ArrayList<Object> collection9 = new ArrayList<>();
    private final Collection<Object> view9 = Collections.unmodifiableCollection(collection9);

    // todo Why does this have other syntax????
    public final List<Integer> collection10 = new ArrayList<>();
    private final List<Integer> view10 = Collections.unmodifiableList(collection10);

    public volatile Map<Object, Object> collection11 = new TreeMap<>();
    private final Map<Object, Object> view11 = Collections.unmodifiableMap(collection11);

    public SortedMap<Object, Object> collection12 = new TreeMap<>();
    private final Map<Object, Object> view12 = Collections.unmodifiableSortedMap(collection12);

    public NavigableMap<Object, Object> collection13 = new TreeMap<>();
    private final Map<Object, Object> view13 = Collections.unmodifiableNavigableMap(collection13);

    public static Set<Object> collection14 = new HashSet<>();
    private final Set<Object> view14 = Collections.unmodifiableSet(collection14);

    public static SortedSet<Object> collection15 = new TreeSet<>();
    private final Set<Object> view15 = Collections.unmodifiableSortedSet(collection15);

    public static NavigableSet<Object> collection16 = new TreeSet<>();
    private final Set<Object> view16 = Collections.unmodifiableNavigableSet(collection16);


    // checked collections
    public ArrayList<Object> collection17 = new ArrayList<>();
    private final Collection<Object> view17 = Collections.checkedCollection(collection17, Object.class);

    public List<Object> collection18 = new ArrayList<>();
    private final List<Object> view18 = Collections.checkedList(collection18, Object.class);

    public Map<Object, Object> collection19 = new HashMap<>();
    private final Map<Object, Object> view19 = Collections.checkedMap(collection19, Object.class, Object.class);

    public SortedMap<Object, Object> collection20 = new TreeMap<>();
    private final Map<Object, Object> view20 = Collections.checkedSortedMap(collection20, Object.class, Object.class);

    public NavigableMap<Object, Object> collection21 = new TreeMap<>();
    private final Map<Object, Object> view21 = Collections.checkedNavigableMap(collection21, Object.class, Object.class);

    public Set<Object> collection22 = new HashSet<>();
    private final Set<Object> view22 = Collections.checkedSet(collection22, Object.class);

    public SortedSet<Object> collection23 = new TreeSet<>();
    private final Set<Object> view23 = Collections.checkedSortedSet(collection23, Object.class);

    public NavigableSet<Object> collection24 = new TreeSet<>();
    private final Set<Object> view24 = Collections.checkedNavigableSet(collection24, Object.class);

    public Queue<Object> collection25 = new PriorityQueue<>();
    private final Queue<Object> view25 = Collections.checkedQueue(collection25, Object.class);


    // Primitive collections
    public Object[] collection26 = new Object[10];
    private final List<Object> view26 = Arrays.asList(collection26);

    public int[] collection27 = new int[10];
    private final List<Object> view27 = Arrays.asList(collection27);


//     Synchronizations on the views
    public void doStuff1() {
        synchronized (view1) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff1Again() {
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

    public void doStuff5() {
        synchronized (view5) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    // These are only for experimenting
    public void doStuff6() {
        synchronized (view6) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff7() {
        synchronized (view7) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff8() {
        synchronized (view8) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff9() {
        synchronized (view9) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff10() {
        synchronized (view10) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff11() {
        synchronized (view11) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff12() {
        synchronized (view12) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff13() {
        synchronized (view13) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff14() {
        synchronized (view14) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff15() {
        synchronized (view15) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff16() {
        synchronized (view16) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff17() {
        synchronized (view17) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff18() {
        synchronized (view18) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff19() {
        synchronized (view19) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff20() {
        synchronized (view20) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff21() {
        synchronized (view21) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff22() {
        synchronized (view22) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff23() {
        synchronized (view23) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff24() {
        synchronized (view24) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff25() {
        synchronized (view25) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff26() {
        synchronized (view26) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }

    public void doStuff27() {
        synchronized (view27) { /* using view as lock with a public backing collection detect bug here */
            System.out.println("Do stuff");
        }
    }
}
