package ghIssues;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Issues 4399 and 4411: a method that acquires a lock and never calls
 * {@code unlock()} itself leaves the lock to be released elsewhere. The caller
 * releases it after a {@code beginRead()}/{@code endRead()} pair, as in Apache
 * Commons Configuration's {@code ReadWriteSynchronizer}, after a private helper
 * called from a {@code try}/{@code finally} block, or when it hands back the
 * object it got under the lock. A method can also leave the release to a
 * method it calls, or to the object it returns.
 */
public class Issue4399 {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock activationLock = new ReentrantLock();

    private final Lock connectionLock = new ReentrantLock();

    private Object connection;

    public void beginRead() {
        lock.readLock().lock();
    }

    public void endRead() {
        lock.readLock().unlock();
    }

    public void beginWrite() {
        lock.writeLock().lock();
    }

    public void endWrite() {
        lock.writeLock().unlock();
    }

    public void activate(Runnable action) {
        acquire();
        try {
            action.run();
        } finally {
            release();
        }
    }

    private void acquire() {
        activationLock.lock();
    }

    private void release() {
        activationLock.unlock();
    }

    public Object get() {
        connectionLock.lock();
        if (connection == null) {
            connection = new Object();
        }
        return connection;
    }

    public void put(Object used) {
        connectionLock.unlock();
    }

    public void write(Runnable task) {
        connectionLock.lock();
        runAndRelease(task);
    }

    private void runAndRelease(Runnable task) {
        try {
            task.run();
        } finally {
            connectionLock.unlock();
        }
    }

    public AutoCloseable open() {
        connectionLock.lock();
        return connectionLock::unlock;
    }
}
