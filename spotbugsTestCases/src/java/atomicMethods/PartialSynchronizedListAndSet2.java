package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PartialSynchronizedListAndSet2 {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());
    private final Set<Integer> nums2 = Collections.synchronizedSet(new HashSet<>());

    public void addAndPrintNumbers(Integer number) {
        synchronized (nums) {
            nums.add(number);
            Integer[] numberCopy = nums.toArray(new Integer[0]);
            // Iterate through array numberCopy ...
        }

        nums2.add(number);
        Integer[] numberCopy2 = nums2.toArray(new Integer[0]);
    }
}
