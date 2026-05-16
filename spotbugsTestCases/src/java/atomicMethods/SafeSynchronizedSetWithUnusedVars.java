package atomicMethods;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SafeSynchronizedSetWithUnusedVars {

    private final Set<Integer> nums = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> nums2 = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> nums3 = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> nums4 = Collections.synchronizedSet(new HashSet<>());

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
