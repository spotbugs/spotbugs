package instanceLockOnSharedStaticData;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class StaticLockObjectOnStaticSharedData {
    private static int counter;
    private static final Object lock = new Object();

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void methodWithoutBugs() {
        synchronized (lock) {
            counter++;
        }
    }

}
