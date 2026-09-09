package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BadExternalizableTest6 implements Externalizable {

    private String name;
    private int UID;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        Object initialized = null;
        if (initialized == null) {
            name = (String) in.readObject();
            UID = in.readInt();
            System.out.printf("name: %s, UID: %d, localVariable: %d%n", name, UID);

            initialized = new Object();
        } else {
            throw new IllegalStateException();
        }
    }
}
