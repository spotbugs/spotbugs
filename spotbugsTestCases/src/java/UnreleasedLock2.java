import java.util.concurrent.locks.ReentrantLock;

public class UnreleasedLock2 {

    private final ReentrantLock lock = new ReentrantLock();

    void doNotReport() {
        lock.lock();
        try {
        } finally {
            lock.unlock();
        }
    }

}
