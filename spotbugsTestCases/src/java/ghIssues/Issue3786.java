package ghIssues;

public class Issue3786 {
    final Object saveLock = new Object();
    volatile double data;

    public void executeFalsePositive() {
        changeData();
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
    }

    public void execute1FalsePositive() {
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

    public void executeTruePositive() {
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
    }

    private void changeData() {
        data = Math.random();
    }
}
