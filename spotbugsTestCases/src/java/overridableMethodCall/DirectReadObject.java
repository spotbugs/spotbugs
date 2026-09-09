package overridableMethodCall;

import java.io.ObjectInputStream;

public class DirectReadObject {

    private void readObject(final ObjectInputStream stream) {
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
