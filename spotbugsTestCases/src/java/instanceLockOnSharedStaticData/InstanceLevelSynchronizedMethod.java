package instanceLockOnSharedStaticData;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class InstanceLevelSynchronizedMethod {
    private static volatile int counter;

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public synchronized void methodWithBug() {
        counter++;
    }
}
