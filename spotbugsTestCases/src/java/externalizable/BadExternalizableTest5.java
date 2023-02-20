package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BadExternalizableTest5 implements Externalizable {

    private final Object lock = new Object();
    private String name;
    private int UID;
    private boolean initialized = false;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        synchronized (lock) {
            if (!initialized) {
                name = (String) in.readObject();
                UID = in.readInt();
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
