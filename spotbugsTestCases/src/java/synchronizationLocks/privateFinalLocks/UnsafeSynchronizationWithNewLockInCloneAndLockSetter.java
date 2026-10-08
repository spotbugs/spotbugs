package synchronizationLocks.privateFinalLocks;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4320">GitHub issue #4320</a>
 */
public class UnsafeSynchronizationWithNewLockInCloneAndLockSetter implements Cloneable {
    private int property;

    private Object lock = new Object();

    @Override
    protected UnsafeSynchronizationWithNewLockInCloneAndLockSetter clone() {
        try {
            UnsafeSynchronizationWithNewLockInCloneAndLockSetter clone = (UnsafeSynchronizationWithNewLockInCloneAndLockSetter) super.clone();
            clone.lock = new Object();
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void setLock(Object lock) {
        this.lock = lock; // exposes the lock to untrusted code
    }

    public void doStuff() {
        synchronized (lock) { /* detect bug here */
            property = 0;
        }
    }
}
