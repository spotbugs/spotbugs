package overridableMethodCall;

public final class FinalClassDoubleIndirect implements Cloneable {
    final void indirect1() {
        finalMethod();
        indirect2();
        privateMethod();
    }

    final void indirect2() {
        indirect1();
        overridableMethod();
    }

    FinalClassDoubleIndirect() {
        indirect1();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    FinalClassDoubleIndirect(FinalClassDoubleIndirect other) {
        other.indirect1();
    }

    @Override
    public FinalClassDoubleIndirect clone() throws CloneNotSupportedException {
        FinalClassDoubleIndirect omc = (FinalClassDoubleIndirect) super.clone();
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
