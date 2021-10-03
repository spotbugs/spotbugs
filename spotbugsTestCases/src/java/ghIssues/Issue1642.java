package ghIssues;

import javax.annotation.Nonnull;

public class Issue1642 {

    @Nonnull static Object a;
    @Nonnull static Object b;
    @Nonnull static Object c;
    @Nonnull static Object d;
    @Nonnull Object x;
    @Nonnull Object y;

    static {
        c = c;
        d = a;
        a = "a";
    }

    Issue1642() {
        x = y = "a";
    }

    Issue1642(String a) {
        x = a;
    }
    
    Issue1642(int z) {
        this();
    }

    Issue1642(double z) {
        super();
    }
    
}
