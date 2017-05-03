import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class TryLock {
    public static void main(String args[]) throws Exception {
        ReentrantLock lock1 = new ReentrantLock();
        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        Lock lock2 = rwLock.readLock();
        Lock lock3 = rwLock.writeLock();
        rwLock.readLock();

        lock1.newCondition();
        lock2.newCondition();
        lock1.tryLock();
        lock2.tryLock();
        lock3.tryLock();

        synchronized (lock1) {
            System.out.println("Howdy");
        }
    }
}
