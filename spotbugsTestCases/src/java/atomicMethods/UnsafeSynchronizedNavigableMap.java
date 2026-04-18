package atomicMethods;

import java.util.Collections;
import java.util.NavigableMap;

public final class UnsafeSynchronizedNavigableMap {

    private final NavigableMap<String, Integer> nums = Collections.synchronizedNavigableMap(Collections.emptyNavigableMap());

    public void addAndPrintNumbers(Integer number) {
        nums.put("key", number);
        Integer[] numberCopy = nums.values().toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
