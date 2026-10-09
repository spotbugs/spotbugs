package atomicMethods;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SafeSynchronizedMapIncrement {

    private final Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());
    private final Object lock = new Object();

    public void increment(String key) {
        synchronized (lock) {
            Integer old = map.get(key);
            int oldValue = (old == null) ? 0 : old;
            if (oldValue == Integer.MAX_VALUE) {
                throw new ArithmeticException("Out of range");
            }
            map.put(key, oldValue + 1);
        }
    }

    public Integer getCount(String key) {
        synchronized (lock) {
            return map.get(key);
        }
    }
}
