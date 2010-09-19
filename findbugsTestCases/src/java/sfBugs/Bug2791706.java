package sfBugs;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Bug2791706 {
    public void clear() {
        Lock l = new ReentrantLock();
        l.lock();
        try {
            // do nothing
        } finally {
            l.unlock();
        }
    }
}
