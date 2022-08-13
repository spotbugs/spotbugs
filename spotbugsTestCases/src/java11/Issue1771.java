package ghIssues;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Issue1771 {
    public Issue1771(List<String> list, Map<String, String> map, Set<String> set) {
        this.list = List.copyOf(list);
        this.map = Map.copyOf(map);
        this.set = Set.copyOf(set);

        list2 = List.of("foo", "bar");
        map2 = Map.of("FOO", "foo", "BAR", "bar");
        set2 = Set.of("foo", "bar");
    }

    private final List<String> list;
    private final Map<String, String> map;
    private final Set<String> set;

    private final List<String> list2;
    private final Map<String, String> map2;
    private final Set<String> set2;

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
}
