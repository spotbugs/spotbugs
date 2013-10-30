package jsr305;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestFooNoControlFlow2 {

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

    @ExpectWarning("TQ")
    public void test12() {
        Object notFoo2 = notFoo;
        foo = notFoo2;
    }

    @ExpectWarning("TQ")
    public void test32() {
        Object returnsNotFoo = returnsNotFoo();
        foo = returnsNotFoo;
    }

    @ExpectWarning("TQ")
    public void test21() {
        Object foo2 = foo;
        notFoo = foo2;
    }

    @ExpectWarning("TQ")
    public void test41() {
        Object returnsFoo = returnsFoo();
        notFoo = returnsFoo;
    }

    @ExpectWarning("TQ")
    public void test16() {
        Object notFoo2 = notFoo;
        requiresFoo(notFoo2);
    }

    @ExpectWarning("TQ")
    public void test36() {
        Object returnsNotFoo = returnsNotFoo();
        requiresFoo(returnsNotFoo);
    }

    @ExpectWarning("TQ")
    public void test25() {
        Object foo2 = foo;
        requiresNotFoo(foo2);
    }

    @ExpectWarning("TQ")
    public void test45() {
        Object returnsFoo = returnsFoo();
        requiresNotFoo(returnsFoo);
    }

    @NoWarning("TQ")
    @ExpectWarning("SA_FIELD_SELF_ASSIGNMENT")
    public void ok22() {
        Object foo2 = foo;
        foo = foo2;
    }

    @NoWarning("TQ")
    public void ok42() {
        Object returnsFoo = returnsFoo();
        foo = returnsFoo;
    }

    @NoWarning("TQ")
    @ExpectWarning("SA_FIELD_SELF_ASSIGNMENT")
    public void ok11() {
        Object notFoo2 = notFoo;
        notFoo = notFoo2;
    }

    @NoWarning("TQ")
    public void ok31() {
        Object returnsNotFoo = returnsNotFoo();
        notFoo = returnsNotFoo;
    }

    @NoWarning("TQ")
    public void ok26() {
        Object foo2 = foo;
        requiresFoo(foo2);
    }

    @NoWarning("TQ")
    public void ok46() {
        Object returnsFoo = returnsFoo();
        requiresFoo(returnsFoo);
    }

    @NoWarning("TQ")
    public void ok15() {
        Object notFoo2 = notFoo;
        requiresNotFoo(notFoo2);
    }

    @NoWarning("TQ")
    public void ok35() {
        Object returnsNotFoo = returnsNotFoo();
        requiresNotFoo(returnsNotFoo);
    }

}
