import java.util.concurrent.locks.ReentrantLock;

public class UnreleasedLock {

	private final ReentrantLock lock = new ReentrantLock();

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
