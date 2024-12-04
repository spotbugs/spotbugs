package multithreaded.compoundoperation;

import java.util.concurrent.atomic.AtomicInteger;

public class CompoundIOROperationOnSharedVariable {
    private AtomicInteger atomicInteger; // just to signal that its multithreaded
    private int num = 1;

    public void toggle() {
        num |= 3;
    }

    public double getNum() {
        return num;
    }
}
