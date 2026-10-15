package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SafeSynchronizedListWithPrivateMethods {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void addAndPrintNumbers(Integer number) {
        addNumber(number);
        Integer[] numberCopy = getArray();
        for (Integer num : numberCopy) {
            System.out.println(num);
        }
    }

    private void addNumber(Integer number) {
        nums.add(number);
    }

    private Integer[] getArray() {
        return nums.toArray(new Integer[0]);
    }
}
