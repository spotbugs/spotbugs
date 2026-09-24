package atomicMethods;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnsafeSynchronizedMapIncrement {

    private final Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

    public void increment(String key) {
        Integer old = map.get(key);
        int oldValue = (old == null) ? 0 : old;
        if (oldValue == Integer.MAX_VALUE) {
            throw new ArithmeticException("Out of range");
        }
        map.put( key, oldValue + 1);
    }

    public Integer getCount(String key) {
        return map.get(key);
    }
}
