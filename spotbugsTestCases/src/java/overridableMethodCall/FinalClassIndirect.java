package overridableMethodCall;

public final class FinalClassIndirect implements Cloneable {
    final void indirect() {
        overridableMethod();
    }

    FinalClassIndirect() {
        indirect();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    FinalClassIndirect(FinalClassIndirect other) {
        other.indirect();
    }

    @Override
    public FinalClassIndirect clone() throws CloneNotSupportedException {
        FinalClassIndirect omc = (FinalClassIndirect) super.clone();
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
