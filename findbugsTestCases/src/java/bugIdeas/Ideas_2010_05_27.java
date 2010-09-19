package bugIdeas;

public class Ideas_2010_05_27 {

    Ideas_2010_05_27 foo() {
        return new Ideas_2010_05_27();
    }

    static class A extends Ideas_2010_05_27 {
        @Override
        A foo() {
            return new A();
        }
    }

    static class B extends Ideas_2010_05_27 {
    }

    // public static boolean test(A a, B b) {
    // return a.foo().equals(b);
    // }
    public static boolean test2(A a, B b) {
        Ideas_2010_05_27 o = a;
        return o.foo().equals(b);
    }

}
