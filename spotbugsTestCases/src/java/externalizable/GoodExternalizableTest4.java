package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GoodExternalizableTest4 implements Externalizable {

    private Object result = null;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public final void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (result == null) {
            // stuff and then ...
            result = new Object();
        } else {
            throw new IllegalStateException("already initialized");
        }
    }
}
