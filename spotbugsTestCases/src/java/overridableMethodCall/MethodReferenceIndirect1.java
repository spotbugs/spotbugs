package overridableMethodCall;

import java.util.Optional;

public class MethodReferenceIndirect1 {
    int l, m, n;
    Optional<Integer> o;

    final int indirect() {
        return overridableMethod();
    }

    MethodReferenceIndirect1(Optional<Integer> opt) {
        o = opt;
        l = o.orElseGet(this::indirect);
        m = o.orElseGet(this::privateMethod);
        n = o.orElseGet(this::finalMethod);
    }

    @Override
    public MethodReferenceIndirect1 clone() throws CloneNotSupportedException {
        MethodReferenceIndirect1 omc = (MethodReferenceIndirect1) super.clone();
        omc.o = o;
        omc.l = o.orElseGet(omc::indirect);
        omc.m = o.orElseGet(omc::privateMethod);
        omc.n = o.orElseGet(omc::finalMethod);
        return omc;
    }

    int overridableMethod() {
        return 1;
    }

    private int privateMethod() {
        return 2;
    }

    final int finalMethod() {
        return 3;
    }
}
