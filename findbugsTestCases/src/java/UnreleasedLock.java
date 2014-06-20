import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class UnreleasedLock {

    private final ReentrantLock lock = new ReentrantLock();

    class Inner {
        @ExpectWarning("IMA_INEFFICIENT_MEMBER_ACCESS")
        void doNotReport() {
            lock.lock();
            try {
            } finally {
                lock.unlock();
            }
        }
    }
}
