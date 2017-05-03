package npe;

import java.io.IOException;

public class GuaranteedDereference2 {
    static void f() throws IOException {
    }

    static int g() {
        Object x = null;
        try {
            f();
            x = new Object();
            int tmp = x.hashCode();
        } catch (IOException e) {
            // ignore it
        }
        return x.hashCode();
    }

    static int g2() {
        Object x = null;
        try {
            f();
            x = new Object();
            f();
            int tmp = x.hashCode();
        } catch (IOException e) {
            // ignore it
        }
        return x.hashCode();
    }
}
