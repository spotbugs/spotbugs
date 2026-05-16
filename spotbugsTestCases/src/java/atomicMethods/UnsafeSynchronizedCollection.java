package atomicMethods;

import java.util.Collection;
import java.util.Collections;

public final class UnsafeSynchronizedCollection {

    private final Collection<Object> nums = Collections.synchronizedCollection(Collections.emptyList());

    public void addAndPrintNumbers(Integer number) {
        nums.add(number);
        Integer[] numberCopy = nums.toArray(new Integer[0]);
        // Iterate through array numberCopy ...
    }
}
