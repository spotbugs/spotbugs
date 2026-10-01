package ghIssues;

public class Issue4323 {
    private final Object first = new Object();
    private final Object second = new Object();

    public void waitWithTwoLocks() throws InterruptedException {
        synchronized (first) {
            synchronized (second) {
                second.wait();
            }
        }
    }
}
