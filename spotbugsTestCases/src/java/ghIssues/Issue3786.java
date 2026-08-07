package ghIssues;

/**
 * Reproducer for issue #3786: NN_NAKED_NOTIFY false positive when state is
 * updated in the same method before the synchronized block that only notifies.
 */
public class Issue3786 {
    final Object saveLock = new Object();
    volatile double data;
    boolean nonVolatile;

    public void execute() {
        changeData();
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
    }

    public void execute1() {
        data = Math.random();
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
    }

    public void executeOk() {
        synchronized (saveLock) {
            changeData();
            saveLock.notifyAll();
        }
    }

    public void execute1Ok() {
        synchronized (saveLock) {
            data = Math.random();
            saveLock.notifyAll();
        }
    }

    public void nakedNotify() {
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
    }

    /** Non-volatile field write before notify remains a true positive. */
    public void nonVolatileBeforeNotify() {
        nonVolatile = true;
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
    }

    private void changeData() {
        data = Math.random();
    }
}
