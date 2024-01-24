package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SynchronizedListInSynchronizedMethod {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public synchronized void addAndPrintNumbers(Integer number) {
        nums.add(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
