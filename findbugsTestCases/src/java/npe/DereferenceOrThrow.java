package npe;

public class DereferenceOrThrow {
    
   /*
    static int f0(Object x, boolean b) {
       if (b) throw new IllegalArgumentException();
        return x.hashCode();
    }
    */


    static int falsePositive(Object x) {
        int result = 0;
        if (x == null) result += 1;
        if (result > 0) throw new IllegalArgumentException();
        return x.hashCode();
    }
    /*
 void fail() {
        throw new RuntimeException();
    }

    int f2(Object x, boolean b) {
        int result = 0;
        if (x == null) result += 1;
        if (result >= 0) fail();
        return x.hashCode();
    }
    */

}
