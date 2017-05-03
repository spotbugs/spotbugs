package bugIdeas;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Ideas_2009_11_10 {

    /**
     * Bug to mutate object after it has been added to a set or map
     */
    static <K> void bug(Set<Collection<K>> set, Collection<K> c, K k) {
        set.add(c);
        c.add(k);
    }

    static <K> void bug2(Set<Collection<K>> set, Collection<K> c, K k) {
        set.add(c);
        c.remove(k);
    }

    static <K> void bug2(Set<List<K>> set, List<K> c, K k, int anyInt) {
        set.add(c);
        c.remove(anyInt);
    }

    static <K, V> void bug(Map<Collection<K>, V> map, Collection<K> c, K k, V v) {
        map.put(c, v);
        c.add(k);
    }
}
