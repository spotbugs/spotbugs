package overridableMethodCall;

import java.util.Optional;

public final class FinalClassInheritedMethodReference implements InterfaceWithDefaultMethod {
    int l, m, n;
    Optional<Integer> o;

    FinalClassInheritedMethodReference(Optional<Integer> opt) {
        o = opt;
        l = o.orElseGet(this::overridableDefaultMethod);
    }

    @Override
    public FinalClassInheritedMethodReference clone() throws CloneNotSupportedException {
        FinalClassInheritedMethodReference omc = (FinalClassInheritedMethodReference) super.clone();
        omc.o = o;
        omc.l = o.orElseGet(omc::overridableDefaultMethod);
        return omc;
    }
}
