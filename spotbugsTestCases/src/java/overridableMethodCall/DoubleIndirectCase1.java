package overridableMethodCall;

public class DoubleIndirectCase1 implements Cloneable {
    final void indirect1() {
        finalMethod();
        indirect2();
        privateMethod();
    }

    final void indirect2() {
        indirect1();
        overridableMethod();
    }

    DoubleIndirectCase1() {
        indirect1();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    DoubleIndirectCase1(DoubleIndirectCase1 other) {
        other.indirect1();
    }

    @Override
    public DoubleIndirectCase1 clone() throws CloneNotSupportedException {
        DoubleIndirectCase1 omc = (DoubleIndirectCase1) super.clone();
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
