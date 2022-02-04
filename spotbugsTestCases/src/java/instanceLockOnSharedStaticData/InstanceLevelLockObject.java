package instanceLockOnSharedStaticData;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class InstanceLevelLockObject {
    private static int counter;
    private final Object lock = new Object();

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void methodWithBug() {
        synchronized (lock) {
            counter++;
        }
    }
}
