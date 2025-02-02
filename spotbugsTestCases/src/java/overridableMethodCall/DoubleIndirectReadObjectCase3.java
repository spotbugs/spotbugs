package overridableMethodCall;

import java.io.ObjectInputStream;

public class DoubleIndirectReadObjectCase3 {
    final void indirect1() {
        finalMethod();
        indirect2();
        privateMethod();
    }

    void readObject(final ObjectInputStream stream) {
        indirect1();
        privateMethod();
        finalMethod();
    }

    final void indirect2() {
        indirect1();
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
