package overridableMethodCall;

import java.util.Optional;

public class MethodReference {
    int l, m, n;
    Optional<Integer> o;

    MethodReference(Optional<Integer> opt) {
        o = opt;
        l = o.orElseGet(this::overridableMethod);
        m = o.orElseGet(this::privateMethod);
        n = o.orElseGet(this::finalMethod);
    }

    @Override
    public MethodReference clone() throws CloneNotSupportedException {
        MethodReference omc = (MethodReference) super.clone();
        omc.o = o;
        omc.l = o.orElseGet(omc::overridableMethod);
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
