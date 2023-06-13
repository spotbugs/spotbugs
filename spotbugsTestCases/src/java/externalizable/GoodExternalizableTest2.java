package externalizable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GoodExternalizableTest2 implements Externalizable {

    private String name;
    private int UID;
    private boolean initialized = false;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int localVariable = 56;

        if (!initialized) {
            name = (String) in.readObject();
            UID = in.readInt();
            System.out.printf("name: %s, UID: %d, localVariable: %d%n", name, UID, localVariable);

            initialized = true;
        } else {
            throw new IllegalStateException();
        }
    }
}
