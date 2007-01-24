package npe;

public class UnconditionalThrower {
    
    static class A {
        void foo() {
            throw new IllegalArgumentException();
        }
    }
    static class B extends A {
        @Override
        void foo() {
            throw new IllegalStateException();
        }
    }

    int falsePositive(Object x, A a) {
        if (x == null) a.foo();
        return x.hashCode();
    }
}
