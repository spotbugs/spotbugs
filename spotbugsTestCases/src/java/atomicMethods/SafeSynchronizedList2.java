package atomicMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SafeSynchronizedList2 {

    private final List<Integer> nums = Collections.synchronizedList(new ArrayList<>());

    public void add(Integer number) {
        synchronized (nums) {
            nums.add(number);
        }
    }

    public void add2(Integer number) {
        synchronized (nums) {
            nums.add(number);
        }
    }

    public void add3(Integer number) {
        nums.add(number);
    }
}
