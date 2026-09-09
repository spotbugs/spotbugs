package overridableMethodCall;

public class IndirectCase1 implements Cloneable {
    final void indirect() {
        overridableMethod();
    }

    IndirectCase1() {
        indirect();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    IndirectCase1(IndirectCase1 other) {
        other.indirect();
    }

    @Override
    public IndirectCase1 clone() throws CloneNotSupportedException {
        IndirectCase1 omc = (IndirectCase1) super.clone();
        omc.indirect();
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
