package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GoodExternalizableTest3 implements Externalizable {

    private Object result;
    private boolean initialized;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public final void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        // stuff and then ...
        result = new Object();
        initialized = true;
    }
}
