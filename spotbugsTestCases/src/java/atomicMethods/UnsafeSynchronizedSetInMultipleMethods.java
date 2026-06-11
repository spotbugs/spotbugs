package atomicMethods;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class UnsafeSynchronizedSetInMultipleMethods {

    private final Set<Integer> nums = Collections.synchronizedSet(new HashSet<>());

    public void addAndPrintNumbers(Integer number) {
        nums.add(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }

    public void removeAndPrintNumbers(Integer number) {
        nums.remove(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
