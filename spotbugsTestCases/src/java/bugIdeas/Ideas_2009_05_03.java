package bugIdeas;

public class Ideas_2009_05_03 {

    static class A {
    };

    static class B {
    };

    int falsePosiive(boolean b) {
        Object x = b ? new A() : new B();

        if (b)
            return ((A) x).hashCode();
        return ((B) x).hashCode();

    }

}
