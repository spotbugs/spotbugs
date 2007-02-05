public class TestFields {

    Object x;
    TestFields(Object x) {
        this.x = x;
    }
    int a(int level) {
        x = null;
        if (level > 0)
            x = new Object();
        if (level > 4)
            return x.hashCode();
        return 0;
    }

    int b(boolean b) {
        x = null;
        if (b)
            x = new Object();
        if (b)
            return x.hashCode();
        return 0;
    }

    int c(boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }

    int d(boolean b) {
        x = null;
        if (b)
            x = new Object();
        return helper1(b);
    }

    int e() {
        x = null;
        return helper2();
    }

    int f() {
        if (x == null)
            System.out.println("x is null");
        return helper2();
    }

    int helper1(boolean b) {
        if (b)
            return 0;
        return x.hashCode();
    }

    int helper2() {
        return x.hashCode();
    }

}
