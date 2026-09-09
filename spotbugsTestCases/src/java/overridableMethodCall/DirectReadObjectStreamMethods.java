package overridableMethodCall;

import java.io.IOException;
import java.io.ObjectInputStream;

public class DirectReadObjectStreamMethods {

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        stream.readFields();
    }
}
