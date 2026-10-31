package ghIssues;

import org.jspecify.annotations.NonNull;

public class Issue1642 {

    static @NonNull Object a;
    static @NonNull Object b;
    static @NonNull Object c;
    static @NonNull Object d;
    @NonNull Object x;
    @NonNull Object y;

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
