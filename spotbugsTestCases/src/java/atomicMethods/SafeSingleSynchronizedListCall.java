package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SafeSingleSynchronizedListCall {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void addNumbers(Integer number) {
        nums.add(number);
    }
}
