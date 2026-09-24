package atomicMethods;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SafeSynchronizedSet2 {

    private final Set<Integer> nums = Collections.synchronizedSet(new HashSet<>());

    public void addNumbers(Integer number) {
        nums.add(number);
    }

    public void removeNumbers(Integer number) {
        nums.remove(number);
    }
}
