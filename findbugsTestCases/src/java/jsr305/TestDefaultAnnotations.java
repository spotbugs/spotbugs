package jsr305;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

@DefaultFooParameters
public class TestDefaultAnnotations {

    // parameter "o" must carry a @Foo(when=When.ALWAYS) type qualifier,
    // since that is the default for parameters
    public void requiresFoo(Object o) {

    }

    // violation: @Foo(when=When.NEVER) value passed to method expecting
    // @Foo(when=When.ALWAYS)
    @ExpectWarning("TQ")
    public void violate(@Foo(when = When.NEVER) Object x) {
        requiresFoo(x);
    }

    // @NoWarning("TQ")
    // public void ok(Object x) {
    // requiresFoo(x);
    // }
    //
    // @NoWarning("TQ")
    // public void ok2(@Foo(when=When.ALWAYS) Object x) {
    // requiresFoo(x);
    // }

}
