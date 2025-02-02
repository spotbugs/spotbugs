package overridableMethodCall;

import java.io.ObjectInputStream;

public class IndirectReadObject2 {
    private void readObject(final ObjectInputStream stream) {
        indirect();
        privateMethod();
        finalMethod();
    }

    final void indirect() {
        overridableMethod();
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
