package overridableMethodCall;

import java.io.ObjectInputStream;

public final class FinalClassDirectReadObject {

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

    void finalMethod() {
        System.out.println("I am final.");
    }
}
