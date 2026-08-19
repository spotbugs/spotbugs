package atomicMethods;

import java.util.concurrent.atomic.AtomicBoolean;

public class UnsafeAtomicReference3 {

    protected AtomicBoolean hadException;

    public void preparation() {
        hadException = new AtomicBoolean(false);
        hadException.get();
    }

    public void run() {
        hadException.get();
    }

    public void after() {
        hadException.get();
    }
}
