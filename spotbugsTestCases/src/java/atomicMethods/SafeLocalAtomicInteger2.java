package atomicMethods;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SafeLocalAtomicInteger2 {

    private final ConcurrentMap<String, AtomicInteger> map = new ConcurrentHashMap<>();

    public void increment(String key) {
        AtomicInteger value = new AtomicInteger();
        AtomicInteger old = map.putIfAbsent(key, value);

        if (old != null) {
            value = old;
        }

        value.incrementAndGet();
    }

    public Integer getCount(String key) {
        AtomicInteger value = map.get(key);
        return (value == null) ? null : value.get();
    }
}
