package npe;

public class DereferenceOrThrow {
    
    void fail() {
        throw new RuntimeException();
    }
    int f(Object x, boolean b) {
        int result = 0;
        if (x == null) result += 1;
        if (result >= 0) throw new IllegalArgumentException();
        return x.hashCode();
    }
    
    /*
    int f2(Object x, boolean b) {
        int result = 0;
        if (x == null) result += 1;
        if (result >= 0) fail();
        return x.hashCode();
    }
    */

}
