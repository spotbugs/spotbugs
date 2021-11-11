package overridableMethodCall;

public class DoubleIndirectCase2 implements Cloneable {
    final void indirect2() {
        indirect1();
        overridableMethod();
    }

    final void indirect1() {
        finalMethod();
        indirect2();
        privateMethod();
    }

    DoubleIndirectCase2() {
        indirect1();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    DoubleIndirectCase2(DoubleIndirectCase2 other) {
        other.indirect1();
    }

    @Override
    public DoubleIndirectCase2 clone() throws CloneNotSupportedException {
        DoubleIndirectCase2 omc = (DoubleIndirectCase2) super.clone();
        omc.indirect1();
        omc.privateMethod();
        omc.finalMethod();
        return omc;
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

    private static void staticMethod() {
        System.out.println("I am static.");
    }
}
