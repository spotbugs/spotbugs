package bugIdeas;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Ideas_2011_10_25 {
    static int counter;

    public int getNext() {
        synchronized (getClass()) {
            return counter++;
        }
    }

    static class Subclass extends Ideas_2011_10_25 {
    }

    Lock lock = new ReentrantLock();

    @DesireWarning("")
    public synchronized int getNext2() {
        return counter++;
    }

    int value;

    public int nextValue() {
        synchronized (lock) {
            return value++;

        }
    }

    public void integerLocks() {
        Integer integerUnsafe = -100;
        Integer integerSafe = new Integer(-100);
        synchronized (integerUnsafe) {
        }
    }

    public void integerLocks2() {
        Integer integerUnsafe = -100;
        Integer integerSafe = new Integer(-100);
        synchronized (integerSafe) {
        }
    }

    long last;

    void foo() {
        for (int i = 1; i < 0x00000000FFFFFFFFL; i++) {

        }
    }

    void foo2() {

        if (++last > 0x00000000FFFFFFFFL)
            last = 1;

    }

}
