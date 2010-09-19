package npe;

public class HandleAssert {

    int f(int x) {
        Object y = null;
        if (x == 1)
            y = new String("one");
        else if (x == 2)
            y = Boolean.TRUE;
        else {
            assert false;
        }
        return y.hashCode();
    }

}
