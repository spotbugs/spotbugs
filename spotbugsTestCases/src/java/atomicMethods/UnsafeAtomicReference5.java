package atomicMethods;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UnsafeAtomicReference5 {

    private final AtomicBoolean atomicBoolean = new AtomicBoolean(true);
    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public void incrementAtomicInteger() {
        atomicInteger.updateAndGet(operand -> {
            if (atomicBoolean.get()) {
                System.out.println(operand);
                if (operand >= 100) atomicBoolean.set(false);
            }
            return operand + 1;
        });
    }
}
