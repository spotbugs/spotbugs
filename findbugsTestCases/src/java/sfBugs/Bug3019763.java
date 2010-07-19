package sfBugs;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bug3019763 {
	public void lockTest() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		lock.readLock().lock();
		try {
			System.out.println("testing");
		} finally {
			// lock.readLock().unlock();
		}
	}
}