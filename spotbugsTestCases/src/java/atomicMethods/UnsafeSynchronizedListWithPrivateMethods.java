package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UnsafeSynchronizedListWithPrivateMethods {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void addAndPrintNumbers(Integer number) {
        addNumber(nums, number);
        Integer[] numberCopy = getArray(nums);
        for (Integer num : numberCopy) {
            System.out.println(num);
        }
    }

    private void addNumber(List<Integer> numsList, Integer number) {
        numsList.add(number);
    }

    private Integer[] getArray(List<Integer> numsList) {
        return numsList.toArray(new Integer[0]);
    }
}
