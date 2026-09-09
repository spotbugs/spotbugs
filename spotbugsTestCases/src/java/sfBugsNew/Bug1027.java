package sfBugsNew;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1027 {
    final Lock activationLock = new ReentrantLock();

    volatile boolean active;

    @NoWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void passivate() {
        try {
            activationLock.lock();
            System.out.println("testing the blocker: " + active);
        } finally {
            active = false;
            activationLock.unlock();
        }
    }

    @NoWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void activate() {
        activationLock.lock();
        active = true;
        try {
            System.out.println("testing the blocker: " + active);
        } finally {
            activationLock.unlock();
        }
    }

    @NoWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
    public void toggle() {
        activationLock.lock();
        active = !active;
        try {
            System.out.println("testing the blocker: " + active);
        } finally {
            active = !active;
            activationLock.unlock();
        }
    }

    public Runnable getRunnable() {
        return new Runnable() {
            @Override
            @NoWarning("UL_UNRELEASED_LOCK_EXCEPTION_PATH")
            public void run() {
                activationLock.lock();
                active = !active;
                try {
                    System.out.println("testing the blocker: " + active);
                } finally {
                    active = !active;
                    activationLock.unlock();
                }
            }
        };
    }
}
