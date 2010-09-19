package npe;

public class InterproceduralMethodOrdering {
    public int test1() {
        return a(null);
    }

    public int test2(Object x) {
        if (x == null)
            return a(x);
        return 0;
    }

    public int test3(Object x) {
        if (x == null)
            System.out.println("x is null");
        return a(x);
    }

    private int a(Object x) {
        return z(x);
    }

    private int b(Object x) {
        return y(x);
    }

    private int c(Object x) {
        return x(x);
    }

    private int d(Object x) {
        return w(x);
    }

    private int e(Object x) {
        return x.hashCode();
    }

    private int w(Object x) {
        return e(x);
    }

    private int x(Object x) {
        return d(x);
    }

    private int y(Object x) {
        return c(x);
    }

    private int z(Object x) {
        return b(x);
    }

}
