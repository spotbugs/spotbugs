package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SafeNestedSynchronizedBlock {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());
    private final Object innerLock = new Object();

    public void addAndPrintNumbers(Integer number) {
        synchronized (nums) {
            synchronized (innerLock) {
                // some inner work
            }
            // After inner sync exits, still inside outer sync
            nums.add(number);
            Integer[] numberCopy = nums.toArray(new Integer[0]);
        }
    }
}
