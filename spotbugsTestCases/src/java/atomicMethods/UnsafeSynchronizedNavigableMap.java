package atomicMethods;

import java.util.Collections;
import java.util.Map;

public final class UnsafeSynchronizedNavigableMap {

    private final Map<String, Integer> nums = Collections.synchronizedSortedMap(Collections.emptyNavigableMap());

    public void addAndPrintNumbers(Integer number) {
        nums.put("key", number);
        Integer[] numberCopy = nums.values().toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
