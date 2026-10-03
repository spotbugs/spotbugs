package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SafeSynchronizedListWithMultipleSync {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void addAndPrintNumbers(Integer number) {
        synchronized (nums) {
            nums.add(number);
        }

        int a = 5;
        int b = 6;
        int c = a + b;
        System.out.println(c);

        synchronized (nums) {
            Integer[] numberCopy = nums.toArray(new Integer[0]);
            // Iterate through array numberCopy ...
        }
    }
}
