package bugIdeas;

public class Ideas_2009_04_10 {

    Object g() {
        return "x";
    }

    int f(Object y) {
        Object x = g();
        if (x == null)
            return 0;
        return x.hashCode();
    }

    static class A {
        int foobar(int x) {
            return x;
        }
    }

    static int huh() {
        A a = new A() {
            int foobar(short x) {
                return super.foobar(x) + 1;
            };
        };
        return a.foobar(5);
    }

}
