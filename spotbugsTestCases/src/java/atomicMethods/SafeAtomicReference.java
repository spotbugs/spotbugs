package atomicMethods;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SafeAtomicReference {

    private final AtomicReference<BigInteger> a;
    private final AtomicReference<BigInteger> b;

    public SafeAtomicReference(BigInteger a, BigInteger b) {
        this.a = new AtomicReference<>(a);
        this.b = new AtomicReference<>(b);
    }

    public synchronized void update(BigInteger a, BigInteger b) {
        this.a.set(a);
        this.b.set(b);
    }

    public synchronized BigInteger add() {
        return a.get().add(b.get());
    }
}
