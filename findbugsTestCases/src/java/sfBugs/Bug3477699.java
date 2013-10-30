package sfBugs;

import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3477699 {

    private volatile int x = 0;

    private ReentrantLock l = new ReentrantLock();

    @DesireNoWarning("VO_VOLATILE_INCREMENT")
    public void testA() {
        synchronized (this) {
            x++; // Warning -- False Positive!
        }
    }

    @DesireNoWarning("VO_VOLATILE_INCREMENT")
    public void testB() {
        l.lock();
        try {
            x++; // Warning -- False Positive!
        } finally {
            l.unlock();
        }
    }

    @DesireNoWarning("VO_VOLATILE_INCREMENT")
    public void testC() {

        Foo f = new Foo();
        f.lock();
        try {
            f.x++; // Warning -- False Positive!
        } finally {
            f.unlock();
        }
    }

    private class Foo extends ReentrantLock {
        volatile int x = 0;
    }
}
