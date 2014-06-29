package bugPatterns;

import java.util.concurrent.locks.Lock;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class UL_UNRELEASED_LOCK {

    @DesireWarning("UL_UNRELEASED_LOCK")
    void bug(Lock any) {
        any.lock();
        // any code
    }

    @DesireWarning("UL_UNRELEASED_LOCK")
    void bug2(Lock any) {
        any.lock();
        // any code; might throw exception
        any.unlock();
    }

    @NoWarning("UL_UNRELEASED_LOCK")
    void notBug(Lock any) {
        any.lock();
        try {
            // any code
        } finally {
            any.unlock();
        }
    }

}
