package multithreaded.compoundoperation;

public class SynchronizedMethodCompoundOperationOnSharedVariable extends Thread {
    private boolean flag = true;

    public synchronized void toggle() {
        flag ^= true;
    }

    public synchronized boolean getFlag() {
        return flag;
    }
}
