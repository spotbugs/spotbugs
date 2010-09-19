package jsr305;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestFooGuaranteedControlFlow2 {

    public @Foo(when = When.NEVER)
    Object notFoo;

    public @Foo(when = When.ALWAYS)
    Object foo;

    public @Foo(when = When.NEVER)
    Object returnsNotFoo() {
        return null;
    };

    public @Foo(when = When.ALWAYS)
    Object returnsFoo() {
        return null;
    };

    public void requiresNotFoo(@Foo(when = When.NEVER) Object x) {
    };

    public void requiresFoo(@Foo(when = When.ALWAYS) Object x) {
    };

    boolean b, b2;

    int i;

    public Object unknown;

    @ExpectWarning("TQ")
    public void test12() {
        Object notFoo2 = notFoo;
        if (b)
            notFoo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        foo = notFoo2;
    }

    @ExpectWarning("TQ")
    public void test32() {
        Object returnsNotFoo = returnsNotFoo();
        if (b)
            returnsNotFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        foo = returnsNotFoo;
    }

    @ExpectWarning("TQ")
    public void test21() {
        Object foo2 = foo;
        if (b)
            foo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        notFoo = foo2;
    }

    @ExpectWarning("TQ")
    public void test41() {
        Object returnsFoo = returnsFoo();
        if (b)
            returnsFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        notFoo = returnsFoo;
    }

    @ExpectWarning("TQ")
    public void test16() {
        Object notFoo2 = notFoo;
        if (b)
            notFoo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresFoo(notFoo2);
    }

    @ExpectWarning("TQ")
    public void test36() {
        Object returnsNotFoo = returnsNotFoo();
        if (b)
            returnsNotFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresFoo(returnsNotFoo);
    }

    @ExpectWarning("TQ")
    public void test25() {
        Object foo2 = foo;
        if (b)
            foo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresNotFoo(foo2);
    }

    @ExpectWarning("TQ")
    public void test45() {
        Object returnsFoo = returnsFoo();
        if (b)
            returnsFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresNotFoo(returnsFoo);
    }

    @NoWarning("TQ")
    public void ok22() {
        Object foo2 = foo;
        if (b)
            foo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        foo = foo2;
    }

    @NoWarning("TQ")
    public void ok42() {
        Object returnsFoo = returnsFoo();
        if (b)
            returnsFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        foo = returnsFoo;
    }

    @NoWarning("TQ")
    public void ok11() {
        Object notFoo2 = notFoo;
        if (b)
            notFoo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        notFoo = notFoo2;
    }

    @NoWarning("TQ")
    public void ok31() {
        Object returnsNotFoo = returnsNotFoo();
        if (b)
            returnsNotFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        notFoo = returnsNotFoo;
    }

    @NoWarning("TQ")
    public void ok26() {
        Object foo2 = foo;
        if (b)
            foo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresFoo(foo2);
    }

    @NoWarning("TQ")
    public void ok46() {
        Object returnsFoo = returnsFoo();
        if (b)
            returnsFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresFoo(returnsFoo);
    }

    @NoWarning("TQ")
    public void ok15() {
        Object notFoo2 = notFoo;
        if (b)
            notFoo2 = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresNotFoo(notFoo2);
    }

    @NoWarning("TQ")
    public void ok35() {
        Object returnsNotFoo = returnsNotFoo();
        if (b)
            returnsNotFoo = unknown;
        if (b2)
            i++;
        else
            i--;
        requiresNotFoo(returnsNotFoo);
    }

}
