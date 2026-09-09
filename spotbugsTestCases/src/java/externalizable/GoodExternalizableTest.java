package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GoodExternalizableTest implements Externalizable {

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
            UID = in.readInt();

            initialized = true;
        } else {
            throw new IllegalStateException();
        }
    }
}
