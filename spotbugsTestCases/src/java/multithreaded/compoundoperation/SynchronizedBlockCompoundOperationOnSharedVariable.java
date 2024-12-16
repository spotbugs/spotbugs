package multithreaded.compoundoperation;

public class SynchronizedBlockCompoundOperationOnSharedVariable extends Thread {
    private int num = 0;

    public synchronized void toggle() {
        num += 1;
    }

    public int getFlag() {
        synchronized (SynchronizedBlockCompoundOperationOnSharedVariable.class) {
            return num;
        }
    }
}
