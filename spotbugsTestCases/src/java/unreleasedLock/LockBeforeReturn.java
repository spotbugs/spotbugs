package unreleasedLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockBeforeReturn {
    private final Lock lock = new ReentrantLock();

    public void hold(boolean held) {
        if (held) {
            lock.lock();
            return;
        }
        lock.unlock();
    }
}
