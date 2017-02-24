package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class RFE1603449 {

    int foo(Object a) {
        if (a == null)
            throw new NullPointerException();
        return a.hashCode();
    }

    int foo2(Object a) {
        return a.hashCode();
    }

    int foo3(Object a) {
        return a.hashCode();
    }

    @ExpectWarning("NP")
    int test() {
        Object a = null;
        return foo(a);
    }

    @ExpectWarning("NP")
    int test2() {
        Object a = null;
        return foo2(a);
    }

    @ExpectWarning("NP")
    int test3() {
        Object a = null;
        return foo3(a);
    }

    @ExpectWarning("NP")
    int testa(Object a) {
        if (a == null)
            System.out.println("a is null");
        return foo(a);
    }

    @ExpectWarning("NP")
    int test2a(Object a) {
        if (a == null)
            System.out.println("a is null");
        return foo2(a);
    }

    @ExpectWarning("NP")
    int test3a(Object a) {
        if (a == null)
            System.out.println("a is null");
        return foo3(a);
    }

}
