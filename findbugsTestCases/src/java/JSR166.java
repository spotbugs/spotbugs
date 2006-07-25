import java.util.concurrent.locks.*;

class JSR166 {
	Lock l = new ReentrantLock();
	ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

	int x;

	void l() {
		l.lock();
	}

	void foo() {
		l.lock();
		x++;
		if (x >= 0)
			l.unlock();
	}
	
	ReadWriteLock rwLock =  new ReentrantReadWriteLock();
	
	int counter;
	int readWriteLockTestReadLock() {
		rwLock.readLock().lock();
		try {
		return counter;
		} finally {
			rwLock.readLock().unlock();
		}
		
		
	}

	int readWriteLockTestWriteLock() {
		rwLock.writeLock().lock();
		try {
		return counter++;
		} finally {
			rwLock.writeLock().unlock();
		}
		
		
	}
	void increment() {
		l.lock();
		x++;
		l.unlock();
	}

	void decrement() {
		l.lock();
		try {
			x++;
		} finally {
			l.unlock();
		}
	}

	Object bug1479629() {
		try {
			rwlock.readLock().lock();
			return null;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	Object bug1479629w() {
		try {
			rwlock.writeLock().lock();
			return null;
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	Object bug1479629a(ReadWriteLock lock) {
		try {
			lock.readLock().lock();
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}
	Object bug1479629aw(ReadWriteLock lock) {
		try {
			lock.writeLock().lock();
			return null;
		} finally {
			lock.writeLock().unlock();
		}
	}
	void waitOnCondition(Condition cond) throws InterruptedException {
		while (x == 0) {
			cond.wait();
			cond.wait(1000L);
			cond.wait(1000L, 10);
		}
	}

	void awaitNotInLoop(Condition cond) throws InterruptedException {
		cond.await();
	}
}
