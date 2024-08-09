package exposemutable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3023">#3023</a>.
 */
public class UnmodifiableClass {

    private final List<String> emptyList;
    private final Set<String> emptySet;
    private final Map<String, String> emptyMap;

    private final List<String> copyOfList;
    private final Set<String> copyOfSet;
    private final Map<String, String> copyOfMap;

    private final List<String> list;
    private final Set<String> set;
    private final Map<String, String> map;

    private final List<String> list2;
    private final Set<String> set2;
    private final Map<String, String> map2;

    public UnmodifiableClass(List<String> list, Set<String> set, Map<String, String> map) {
        this.emptyList = Collections.emptyList();
        this.emptySet = Collections.emptySet();
        this.emptyMap = Collections.emptyMap();

        this.copyOfList = List.copyOf(list);
        this.copyOfSet = Set.copyOf(set);
        this.copyOfMap = Map.copyOf(map);

        this.list = list != null ? List.copyOf(list) : Collections.emptyList();
        this.set = set != null ? Set.copyOf(set) : Collections.emptySet();
        this.map = map != null ? Map.copyOf(map) : Collections.emptyMap();

        if (list != null) {
            this.list2 = List.copyOf(list);
        } else {
            this.list2 = Collections.emptyList();
        }

        if (set != null) {
            this.set2 = Set.copyOf(set);
        } else {
            this.set2 = Collections.emptySet();
        }

        if (map != null) {
            this.map2 = Map.copyOf(map);
        } else {
            this.map2 = Collections.emptyMap();
        }
    }

    public List<String> getEmptyList() {
        return emptyList;
    }

    public Set<String> getEmptySet() {
        return emptySet;
    }

    public Map<String, String> getEmptyMap() {
        return emptyMap;
    }

    public List<String> getCopyOfList() {
        return copyOfList;
    }

    public Set<String> getCopyOfSet() {
        return copyOfSet;
    }

    public Map<String, String> getCopyOfMap() {
        return copyOfMap;
    }

    public List<String> getList() {
        return list;
    }

    public Set<String> getSet() {
        return set;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public List<String> getList2() {
        return list2;
    }

    public Set<String> getSet2() {
        return set2;
    }

    public Map<String, String> getMap2() {
        return map2;
    }
}
