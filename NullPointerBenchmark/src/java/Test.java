public class Test {

    int a(int level) {
        Object x = null;
        if (level > 0)
            x = new Object();
        if (level > 4)
            return x.hashCode();
        return 0;
    }

    int b(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        if (b)
            return x.hashCode();
        return 0;
    }

    int c1(Object x, boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }

    int c2(Object x, boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }

    int d(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        return helper1(x, b);
    }

    int e() {
        return helper2(null);
    }

    int f(Object x) {
        if (x == null)
            System.out.println("x is null");
        return helper2(x);
    }

    private int helper1(Object x, boolean b) {
        if (b)
            return 0;
        return x.hashCode();
    }

    private int helper2(Object x) {
        return x.hashCode();
    }

}
