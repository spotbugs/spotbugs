package atomicMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SafeAtomicValueUsage {

    private static AtomicInteger globalInstanceCount = new AtomicInteger( 0 );

    private final List<Integer> cachedValues = new ArrayList<>();
    private final int instanceId = globalInstanceCount.getAndIncrement();

    public void addCachedValueIfNotPresent() {
        if (!isCachedValue(instanceId)) {
            addCachedValue(instanceId);
        }
    }

    private boolean isCachedValue(int value) {
        return cachedValues.contains(value);
    }

    private void addCachedValue(int value) {
        cachedValues.add(value);
    }

    public void removeCachedValue() {
        cachedValues.remove(instanceId);
    }
}
