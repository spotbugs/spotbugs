package ghIssues;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Issue3962 {
    private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    public Object bugTriggerFP() {
        Boolean a = false;

        rwlock.readLock().lock();
        try {
            return null;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    public Object bugTriggerTN() {
        rwlock.readLock().lock();
        try {
            return null;
        } finally {
            rwlock.readLock().unlock();
        }
    }
}
