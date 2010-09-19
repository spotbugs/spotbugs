package bugIdeas.ideas_2010_08_29;

import java.util.HashSet;

public class Test {

    private void test(Object x) {

    }

    private class X {
        X(Object o) {
        }

        public int foo(Object x) {
            return 17;
        }

    }

    private static class Y {
        Y(Object o) {
        }

        static void test(Object x) {
        }

        public int foo(Object x) {
            return 17;
        }
    }

    static HashSet<String> s1;

    static HashSet<String> s2;

    static {
        if (s2 == null) {
            s2 = new HashSet<String>();
            s2.add("a");
        }
    }

    static HashSet<String> getS1() {
        if (s1 == null) {
            s1 = new HashSet<String>();
            s1.add("a");
        }
        return s1;
    }

    public static void main(String args[]) {
        Test test = new Test();
        test.test(null);
        X x = test.new X(null);
        x.foo(null);
        Y y = new Y(null);
        y.foo(null);
        Y.test(null);
    }
}
