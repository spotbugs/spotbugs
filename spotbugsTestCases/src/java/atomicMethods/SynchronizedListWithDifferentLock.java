package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SynchronizedListWithDifferentLock {

    private final Object lock = new Object();
    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void addAndPrintNumbers(Integer number) {
        synchronized (lock) {
            nums.add(number);
            Integer[] numberCopy = nums.toArray(new Integer[0]);
            // Iterate through array numberCopy ...
        }
    }
}
