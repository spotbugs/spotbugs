package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GoodExternalizableTest8 implements Externalizable {

    private String name;
    private int UID;
    private boolean initialized = false;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (name == null || !initialized) {
            UID = in.readInt();

            initialized = true;
        } else {
            throw new IllegalStateException();
        }
    }
}
