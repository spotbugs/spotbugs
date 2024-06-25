package overridableMethodCall;

public final class FinalClassInheritedIndirect implements Cloneable, InterfaceWithDefaultMethod {
    final void indirect() {
        overridableDefaultMethod();
    }

    FinalClassInheritedIndirect() {
        indirect();
    }

    FinalClassInheritedIndirect(FinalClassInheritedIndirect other) {
        other.indirect();
    }

    @Override
    public FinalClassInheritedIndirect clone() throws CloneNotSupportedException {
        FinalClassInheritedIndirect omc = (FinalClassInheritedIndirect) super.clone();
        omc.indirect();
        return omc;
    }
}
