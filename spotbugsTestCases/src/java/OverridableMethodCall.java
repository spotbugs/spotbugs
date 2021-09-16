public class OverridableMethodCall implements Cloneable {
    OverridableMethodCall() {
        overridableMethod();
        privateMethod();
        finalMethod();
        staticMethod();
    }

    OverridableMethodCall(OverridableMethodCall other) {
        other.overridableMethod();
    }

    @Override
    public OverridableMethodCall clone() throws CloneNotSupportedException {
        OverridableMethodCall omc = (OverridableMethodCall) super.clone();
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
