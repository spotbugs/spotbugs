package overridableMethodCall;

public final class FinalClassInheritedDirect implements Cloneable, InterfaceWithDefaultMethod {
    FinalClassInheritedDirect() {
        overridableDefaultMethod();
    }

    FinalClassInheritedDirect(FinalClassInheritedDirect other) {
        other.overridableDefaultMethod();
    }

    @Override
    public FinalClassInheritedDirect clone() throws CloneNotSupportedException {
        FinalClassInheritedDirect omc = (FinalClassInheritedDirect) super.clone();
        omc.overridableDefaultMethod();
        return omc;
    }
}
