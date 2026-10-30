package ghIssues;

import java.util.concurrent.locks.ReentrantLock;

public class Issue4411 {
    private final ReentrantLock activationLock = new ReentrantLock();

    public void test() {
        try {
            acquireLock();
        } finally {
            releaseLock();
        }
    }

    private void releaseLock() {
        activationLock.unlock();
    }

    private void acquireLock() {
        activationLock.lock();
    }
}
