package bugIdeas;

import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import junit.framework.Assert;

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
        Assert.assertTrue(x != null);
        return x.hashCode();
    }

    int f4(Object x) {
        Assert.assertFalse(x == null);
        return x.hashCode();
    }

    @DesireWarning("NP")
    int f3Bug(Object x) {
        Assert.assertTrue(x == null);
        return x.hashCode();
    }

    @DesireWarning("NP")
    int f4Bug(Object x) {
        Assert.assertFalse(x != null);
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
        Assert.assertTrue(o != null);
        return o.hashCode();
    }

    int f5e(Object o) {
        if (o == null)
            System.out.println("oops");
        Assert.assertTrue(o != null);
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

    @CheckForNull
    Object h(Object x) {
        return x;
    }

    int g(Object x) {
        Assert.assertNotNull(x != null);
        return x.hashCode();
    }

    void foo(boolean b) {
    }

    boolean assertIsTrue(boolean b) {
        assert b;
        return b;
    }
}
