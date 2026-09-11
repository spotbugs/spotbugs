package atomicMethods;

import java.util.Collections;
import java.util.Set;

public final class UnsafeSynchronizedNavigableSet {

    private final Set<Integer> nums = Collections.synchronizedNavigableSet(Collections.emptyNavigableSet());

    public void addAndPrintNumbers(Integer number) {
        nums.add(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
