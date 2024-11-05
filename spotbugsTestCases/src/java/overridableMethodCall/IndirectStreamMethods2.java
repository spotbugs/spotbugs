package overridableMethodCall;

import java.io.IOException;
import java.io.ObjectInputStream;

public class IndirectStreamMethods2 {
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        indirect(stream);
    }

    final void indirect(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        stream.readFields();
    }
}
