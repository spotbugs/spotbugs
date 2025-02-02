package multithreaded.compoundoperation;

public class CompoundAdditionOnSharedVolatileVariable extends Thread {
    private volatile int num = 0;

    public void toggle() {
        num += 2;
    }

    public Integer getNum() {
        return num;
    }
}
