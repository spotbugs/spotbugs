import java.util.concurrent.locks.ReentrantLock;

public class UnreleasedLock3 {

    final ReentrantLock lock = new ReentrantLock();

    class Inner {
        void doNotReport() {
            lock.lock();
            try {
            } finally {
                lock.unlock();
            }
        }
    }
}
