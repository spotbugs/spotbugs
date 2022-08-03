package overridableMethodCall;

import java.util.Optional;

public class MethodReferenceIndirect3 {
    int l, m, n;
    Optional<Integer> o;

    final int indirectOverridable() {
        return o.orElseGet(this::overridableMethod);
    }

    final int indirectPrivate() {
        return o.orElseGet(this::privateMethod);
    }

    final int indirectFinal() {
        return o.orElseGet(this::finalMethod);
    }

    MethodReferenceIndirect3(Optional<Integer> opt) {
        o = opt;
        l = o.orElseGet(this::indirectOverridable);
        m = o.orElseGet(this::indirectPrivate);
        n = o.orElseGet(this::indirectFinal);
    }

    @Override
    public MethodReferenceIndirect3 clone() throws CloneNotSupportedException {
        MethodReferenceIndirect3 omc = (MethodReferenceIndirect3) super.clone();
        omc.o = o;
        omc.l = o.orElseGet(omc::indirectOverridable);
        omc.m = o.orElseGet(omc::indirectPrivate);
        omc.n = o.orElseGet(omc::indirectFinal);
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
