package jsr305;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestFooUnknownSource {

    int f(@NeverFoo String c) {
        return c.hashCode();
    }

    int g(@Foo String c) {
        return c.hashCode();
    }

    @NoWarning("TQ")
    int unannotatedSourceToNeverSinkFalsePositive(String c) {
        return f(c); // should not generate a warning here
    }

    @NoWarning("TQ")
    int unannotatedSourceToAlwaysSinkFalsePositive(String c) {
        return g(c); // should not generate a warning here
    }

    @ExpectWarning("TQ")
    int unknownSourceToNeverSinkFalsePositive(@Foo(when = When.UNKNOWN) String c) {
        return f(c); // should generate a warning here
    }

    @ExpectWarning("TQ")
    int unknownSourceToNeverSourceFalsePositive(@Foo(when = When.UNKNOWN) String c) {
        return g(c); // should generate a warning here
    }
}
