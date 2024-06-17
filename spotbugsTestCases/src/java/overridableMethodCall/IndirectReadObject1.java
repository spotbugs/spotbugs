package overridableMethodCall;

import java.io.ObjectInputStream;

public class IndirectReadObject1 {
    final void indirect() {
        overridableMethod();
    }

    private void readObject(final ObjectInputStream stream) {
        indirect();
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
