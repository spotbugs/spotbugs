package overridableMethodCall;

import java.io.IOException;
import java.io.ObjectInputStream;

public class DirectReadObjectStreamMethods2 {

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        stream.readFields();
        overridableMethod();
        privateMethod();
        finalMethod();
    }

    void overridableMethod() {
        System.out.println("I am overridable.");
    }

    private void privateMethod() {
        System.out.println("I am private.");
    }

    final void finalMethod() {
        System.out.println("I am final.");
    }
}
