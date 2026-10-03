package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UnsafeSynchronizedList {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void addAndPrintNumbers(Integer number) {
        nums.add(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        for (Integer num : numberCopy) {
            System.out.println(num);
        }
    }
}
