package unreleasedLock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockWithoutUnlock {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    void test() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
    }
}
