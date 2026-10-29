package ghIssues;

import java.util.concurrent.locks.ReentrantLock;

public class Issue4411 {
    private final ReentrantLock activationLock = new ReentrantLock();

    public void test() {
        try {
            l();
        } finally {
            u();
        }
    }

    private void u() {
        activationLock.unlock();
    }

    private void l() {
        activationLock.lock();
    }
}
