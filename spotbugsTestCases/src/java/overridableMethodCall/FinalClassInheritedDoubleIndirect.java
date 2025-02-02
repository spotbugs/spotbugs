package overridableMethodCall;

public final class FinalClassInheritedDoubleIndirect implements Cloneable, InterfaceWithDefaultMethod {
    final void indirect1() {
        indirect2();
    }

    final void indirect2() {
        indirect1();
        overridableDefaultMethod();
    }

    FinalClassInheritedDoubleIndirect() {
        indirect1();
    }

    FinalClassInheritedDoubleIndirect(FinalClassInheritedDoubleIndirect other) {
        other.indirect1();
    }

    @Override
    public FinalClassInheritedDoubleIndirect clone() throws CloneNotSupportedException {
        FinalClassInheritedDoubleIndirect omc = (FinalClassInheritedDoubleIndirect) super.clone();
        omc.indirect1();
        return omc;
    }
}
