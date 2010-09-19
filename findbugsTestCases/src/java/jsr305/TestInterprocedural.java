package jsr305;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class TestInterprocedural {
    @Foo
    Object fooField;

    public void setFoo(@Foo Object o) {
        fooField = o;
    }

    public @Foo
    Object getFoo() {
        return fooField;
    }

    protected abstract void requireFoo(@Foo Object obj);

    @Foo(when = When.NEVER)
    protected abstract Object notFoo();

    // Requires a value that is always Foo,
    // but is not annotated as such.
    @NoWarning("TQ")
    protected void requiresFooButNotAnnotatedAsSuch(Object o) {
        requireFoo(o);
    }

    // Returns a value that is notFoo,
    // but does not have any direct annotations.
    @NoWarning("TQ")
    public Object g() {
        return notFoo();
    }

    @ExpectWarning("TQ")
    public void report1() {
        Object notFoo = g();
        fooField = notFoo;
    }

    @ExpectWarning("TQ")
    public void report2() {
        Object notFoo = notFoo();
        requiresFooButNotAnnotatedAsSuch(notFoo);
    }
}
