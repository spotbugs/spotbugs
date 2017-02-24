package bugIdeas;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ideas_2009_04_27 {

    Lock lock = new ReentrantLock();

    public void getLock() {
        lock.lock();
    }

    public void releaseLock() {
        lock.unlock();
    }
}
