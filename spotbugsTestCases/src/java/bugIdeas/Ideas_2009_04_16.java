package bugIdeas;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Ideas_2009_04_16<T> {

    ConcurrentHashMap<T, AtomicInteger> map = new ConcurrentHashMap<T, AtomicInteger>();

    AtomicInteger getCounter(T t) {
        AtomicInteger value = map.get(t);
        if (value == null) {
            value = new AtomicInteger();
            map.putIfAbsent(t, value);
        }
        return value;
    }

    void increment(T t) {
        AtomicInteger value = map.get(t);
        if (value == null) {
            value = new AtomicInteger(1);
            value = map.putIfAbsent(t, value);
        }
        if (value != null)
            value.getAndIncrement();

    }

}
