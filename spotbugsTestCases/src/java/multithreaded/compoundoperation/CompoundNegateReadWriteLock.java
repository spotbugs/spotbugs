package multithreaded.compoundoperation;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompoundNegateReadWriteLock extends Thread {
    private boolean flag = true;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public void toggle() {
        writeLock.lock();
        try {
            flag ^= true; // Same as flag = !flag;
        } finally {
            writeLock.unlock();
        }
    }

    public boolean getFlag() {
        readLock.lock();
        try {
            return flag;
        } finally {
            readLock.unlock();
        }
    }
}
