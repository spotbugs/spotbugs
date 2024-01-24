package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SafeSynchronizedLists {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> nums2 = Collections.synchronizedList(new ArrayList<>());

    public void addAndPrintNumbers(Integer number) {
        synchronized (nums) {
            nums.add(number);
            Integer[] numberCopy = nums.toArray(new Integer[0]);
            // Iterate through array numberCopy ...
            nums2.add(number);
            Integer[] numberCopy2 = nums2.toArray(new Integer[0]);
            // Iterate through array numberCopy2 ...
        }
    }
}
