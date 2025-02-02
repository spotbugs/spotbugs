package multithreaded.compoundoperation;

public class CompoundAdditionOnSharedVolatileReadSyncWrite extends Thread {
    private volatile int num = 0;

    public synchronized void toggle() {
        num += 2;
    }

    public Integer getNum() {
        return num;
    }
}
