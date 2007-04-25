import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
		rwlock.readLock().lock();
		try {

			return null;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	Object bug1479629w() {
		rwlock.writeLock().lock();
		try {

			return null;
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	Object bug1479629a(ReadWriteLock lock) {
		lock.readLock().lock();
		try {

			return null;
		} finally {
			lock.readLock().unlock();
		}
	}
	Object bug1479629aw(ReadWriteLock lock) {
		lock.writeLock().lock();
		try {

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

	final Lock fieldLock = new ReentrantLock();

	int y;
	int lockOnFieldDoNotReport(Object f) {
		fieldLock.lock();
		try {
			y += f.hashCode();
			return y;
		} finally {
			fieldLock.unlock();
		}
	}
	int lockOnFieldReport(Object f) {
		fieldLock.lock();
		y += f.hashCode();
		int result = y;
		fieldLock.unlock();
		return result;
	}

}
