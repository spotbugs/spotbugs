package synchronizationLocks.privateFinalLocks;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4320">GitHub issue #4320</a>
 */
public class SafeSynchronizationWithNewLockInClone implements Cloneable {
    private int property;

    private Object lock = new Object();

    @Override
    protected SafeSynchronizationWithNewLockInClone clone() {
        try {
            SafeSynchronizationWithNewLockInClone clone = (SafeSynchronizationWithNewLockInClone) super.clone();
            clone.lock = new Object(); // the clone gets its own lock, nothing is exposed
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void doStuff() {
        synchronized (lock) {
            property = 0;
        }
    }
}
