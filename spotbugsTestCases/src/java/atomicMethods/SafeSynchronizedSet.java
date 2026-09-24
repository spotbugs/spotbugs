package atomicMethods;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SafeSynchronizedSet {

    private final Set<Integer> nums = Collections.synchronizedSet(new HashSet<>());

    public void addNumbers(Integer number) {
        synchronized (nums) {
            nums.add(number);
        }
    }

    public void removeNumbers(Integer number) {
        synchronized (nums) {
            nums.remove(number);
        }
    }
}
