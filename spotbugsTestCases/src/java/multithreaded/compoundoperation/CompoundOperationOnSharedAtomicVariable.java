package multithreaded.compoundoperation;

import java.util.concurrent.atomic.AtomicInteger;

public class CompoundOperationOnSharedAtomicVariable extends Thread {
    private AtomicInteger num = new AtomicInteger(0);

    public void toggle() {
        num.set(num.get() + 2);
    }

    public Integer getNum() {
        return num.get();
    }
}
