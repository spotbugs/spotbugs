package bugIdeas;

import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.DesireWarning;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

public class Ideas_2011_12_19 {

    static final Logger LOG = Logger.getLogger(Ideas_2011_12_19.class.getName());

    int f(Object x) {
        assert x != null;
        return x.hashCode();
    }

    int f2(Object x) {
        assert x == null;
        return x.hashCode();
    }

    int f3(Object x) {
        Assertions.assertTrue(x != null);
        return x.hashCode();
    }

    int f4(Object x) {
        Assertions.assertFalse(x == null);
        return x.hashCode();
    }

    @DesireWarning("NP")
    int f3Bug(Object x) {
        Assertions.assertTrue(x == null);
        return x.hashCode();
    }

    @DesireWarning("NP")
    int f4Bug(Object x) {
        Assertions.assertFalse(x != null);
        return x.hashCode();
    }

    int f5a() {
        Object o = h("a");
        return o.hashCode();
    }

    int f5b() {
        Object o = h("a");
        assert o != null;
        return o.hashCode();
    }

    int f5c() {
        Object o = h("a");
        Assertions.assertTrue(o != null);
        return o.hashCode();
    }

    int f5e(Object o) {
        if (o == null)
            System.out.println("oops");
        Assertions.assertTrue(o != null);
        return o.hashCode();
    }

    int f5f(Object o) {
        if (o == null)
            System.out.println("oops");
        assertIsTrue(o != null);
        return o.hashCode();
    }

    int f5g(Object o) {
        if (o == null)
            System.out.println("oops");
        foo(o != null);
        return o.hashCode();
    }

    @Nullable
    Object h(Object x) {
        return x;
    }

    int g(Object x) {
        Assertions.assertNotNull(x != null);
        return x.hashCode();
    }

    void foo(boolean b) {
    }

    boolean assertIsTrue(boolean b) {
        assert b;
        return b;
    }
}
