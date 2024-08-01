package overridableMethodCall;

public final class FinalClassDirect implements Cloneable {
    FinalClassDirect() {
        overridableMethod();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    FinalClassDirect(FinalClassDirect other) {
        other.overridableMethod();
    }

    @Override
    public FinalClassDirect clone() throws CloneNotSupportedException {
        FinalClassDirect omc = (FinalClassDirect) super.clone();
        omc.overridableMethod();
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
