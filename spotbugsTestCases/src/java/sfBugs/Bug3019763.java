package sfBugs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3019763 {
    ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    ReentrantLock lock = new ReentrantLock();

    @NoWarning("UL_UNRELEASED_LOCK")
    public void doTest() {
        rwLock.readLock().lock();
        try {
            System.out.println("testing");
        } finally {
            // lock.readLock().unlock();
        }
    }

    @NoWarning("UL_UNRELEASED_LOCK")
    public void doTestA() {
        ReadLock readLock = rwLock.readLock();
        readLock.lock();

        System.out.println("testing");

    }

    @ExpectWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void doTest2() {
        rwLock.readLock().lock();

        System.out.println("testing");
        rwLock.readLock().unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void doTest2A() {
        ReadLock readLock = rwLock.readLock();
        readLock.lock();

        System.out.println("testing");
        readLock.unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK")
    public void doTest2(boolean b) {
        rwLock.readLock().lock();

        if (b)
            return;
        System.out.println("testing");
        rwLock.readLock().unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK")
    public void doTest2A(boolean b) {
        ReadLock readLock = rwLock.readLock();
        readLock.lock();

        if (b)
            return;
        System.out.println("testing");
        readLock.unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK")
    public void doTestB(boolean b) {
        lock.lock();

        if (b)
            return;
        System.out.println("testing");
        lock.unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void doTest2(OutputStream o) throws IOException {
        rwLock.readLock().lock();

        o.write(0);
        rwLock.readLock().unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void doTest2A(OutputStream o) throws IOException {
        ReadLock readLock = rwLock.readLock();
        readLock.lock();

        o.write(0);
        readLock.unlock();

    }

    @ExpectWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void doTest2B(OutputStream o) throws IOException {
        lock.lock();

        o.write(0);
        lock.unlock();

    }
}
