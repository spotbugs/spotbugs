package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BadExternalizableTest4 implements Externalizable {

    private String name;
    private int UID;
    private boolean initialized = false;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (!initialized) {
            name = (String) in.readObject();

            initialized = true;
        } else {
            return;
        }
        UID = in.readInt();
    }
}
