package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UnsafeSynchronizedListWithMethodInit {

    private List<Integer> nums;

    public void init() {
        nums = Collections.synchronizedList(new ArrayList<>());
    }

    public void addAndPrintNumbers(Integer number) {
        nums.add(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
