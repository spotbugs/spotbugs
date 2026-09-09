package synchronizationLocks.privateFinalLocks;

import java.io.ObjectStreamException;
import java.io.Serializable;

public class SafeSynchronizationWithBuiltinMethodExposingLock implements Cloneable, Serializable {
    public synchronized void doStuff() {
        System.out.println("Do some stuff");
    }

    @Override
    public SafeSynchronizationWithBuiltinMethodExposingLock clone() {
        try {
            return (SafeSynchronizationWithBuiltinMethodExposingLock) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public Object readResolve() throws ObjectStreamException {
        return this;
    }

}
