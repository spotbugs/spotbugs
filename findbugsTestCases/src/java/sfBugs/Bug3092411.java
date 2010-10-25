package sfBugs;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3092411 {
    private Lock myLock = new ReentrantLock();
    private Condition myCondition = create();

    private Condition create() {
        return null;
    }

    @DesireNoWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    void func1() {
        myLock.lock();
        try {
            System.out.println("hi");
        }
        catch (Exception ex) {
            System.out.println("ex");
        }
        finally {
            myCondition.signalAll();
            myLock.unlock();
        }
    }
}
