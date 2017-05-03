package npe;

public class CallToPanic {

    static native void panic();

    static int foo(Object x) {
        if (x == null)
            panic();
        return x.hashCode();
    }

}
