package ghIssues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class Issue1771 {
    public Issue1771(List<String> list, Map<String, String> map, Set<String> set) {
        this.list = List.copyOf(list);
        this.map = Map.copyOf(map);
        this.set = Set.copyOf(set);

        list2 = List.of("foo", "bar");
        map2 = Map.of("FOO", "foo", "BAR", "bar");
        set2 = Set.of("foo", "bar");

        List<String> l = new ArrayList<>();
        l.add("foo");
        l.add("bar");

        List<String> l2 = new LinkedList<>();
        l2.add("foo");
        l2.add("bar");

        Map<String, String> m = new HashMap();
        m.put("FOO", "foo");
        m.put("BAR", "bar");

        NavigableMap<String, String> nm = new TreeMap();
        nm.put("FOO", "foo");
        nm.put("BAR", "bar");

        SortedMap<String, String> sm = new TreeMap();
        sm.put("FOO", "foo");
        sm.put("BAR", "bar");

        Set<String> s = new HashSet();
        s.add("foo");
        s.add("bar");

        NavigableSet<String> ns = new TreeSet();
        ns.add("foo");
        ns.add("bar");

        SortedSet<String> ss = new TreeSet();
        ss.add("foo");
        ss.add("bar");

        list3 = Collections.unmodifiableList(l);
        list3a = Collections.unmodifiableList(l2);
        map3 = Collections.unmodifiableMap(m);
        navigableMap3 = Collections.unmodifiableNavigableMap(nm);
        sortedMap3 = Collections.unmodifiableSortedMap(sm);
        set3 = Collections.unmodifiableSet(s);
        navigableSet3 = Collections.unmodifiableNavigableSet(ns);
        sortedSet3 = Collections.unmodifiableSortedSet(ss);

        list4 = Collections.singletonList("foo");
        map4 = Collections.singletonMap("FOO", "foo");
        set4 = Collections.singleton("foo");

        list5 = Collections.emptyList();
        map5 = Collections.emptyMap();
        navigableMap5 = Collections.emptyNavigableMap();
        sortedMap5 = Collections.emptySortedMap();
        set5 = Collections.emptySet();
        navigableSet5 = Collections.emptyNavigableSet();
        sortedSet5 = Collections.emptySortedSet();
    }

    private final List<String> list;
    private final Map<String, String> map;
    private final Set<String> set;

    private final List<String> list2;
    private final Map<String, String> map2;
    private final Set<String> set2;

    private final List<String> list3;
    private final List<String> list3a;
    private final Map<String, String> map3;
    private final NavigableMap<String, String> navigableMap3;
    private final SortedMap<String, String> sortedMap3;
    private final Set<String> set3;
    private final NavigableSet<String> navigableSet3;
    private final SortedSet<String> sortedSet3;

    private final List<String> list4;
    private final Map<String, String> map4;
    private final Set<String> set4;

    private final List<String> list5;
    private final Map<String, String> map5;
    private final NavigableMap<String, String> navigableMap5;
    private final SortedMap<String, String> sortedMap5;
    private final Set<String> set5;
    private final NavigableSet<String> navigableSet5;
    private final SortedSet<String> sortedSet5;

    public List<String> getList() {
        return list;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Set<String> getSet() {
        return set;
    }

    public List<String> getList2() {
        return list2;
    }

    public Map<String, String> getMap2() {
        return map2;
    }

    public Set<String> getSet2() {
        return set2;
    }

    public List<String> getList3() {
        return list3;
    }

    public List<String> getList3a() {
        return list3a;
    }

    public Map<String, String> getMap3() {
        return map3;
    }

    public NavigableMap<String, String> getNavigableMap3() {
        return navigableMap3;
    }

    public SortedMap<String, String> getSortedMap3() {
        return sortedMap3;
    }

    public Set<String> getSet3() {
        return set3;
    }

    public NavigableSet<String> getNavigableSet3() {
        return navigableSet3;
    }

    public SortedSet<String> getSortedSet3() {
        return sortedSet3;
    }

    public List<String> getList4() {
        return list4;
    }

    public Map<String, String> getMap4() {
        return map4;
    }

    public Set<String> getSet4() {
        return set4;
    }

    public List<String> getList5() {
        return list5;
    }

    public Map<String, String> getMap5() {
        return map5;
    }

    public NavigableMap<String, String> getNavigableMap5() {
        return navigableMap5;
    }

    public SortedMap<String, String> getSortedMap5() {
        return sortedMap5;
    }

    public Set<String> getSet5() {
        return set5;
    }

    public NavigableSet<String> getNavigableSet5() {
        return navigableSet5;
    }

    public SortedSet<String> getSortedSet5() {
        return sortedSet5;
    }
}
