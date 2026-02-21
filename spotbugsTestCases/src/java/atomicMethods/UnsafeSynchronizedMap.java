package atomicMethods;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UnsafeSynchronizedMap {

    private final Map<String, Integer> nums = Collections.synchronizedMap(new HashMap<>());

    public void addAndPrintNumbers(Integer number) {
        nums.put("key", number);
        Integer[] numberCopy = nums.values().toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
