// Regression test for https://github.com/spotbugs/spotbugs/issues/4323.
public class TwoLockWaitDistinctMonitors {
    private final Object first = new Object();
    private final Object second = new Object();

    void waitWithTwoLocks() throws InterruptedException {
        synchronized (first) {
            synchronized (second) {
                second.wait();
            }
        }
    }
}
