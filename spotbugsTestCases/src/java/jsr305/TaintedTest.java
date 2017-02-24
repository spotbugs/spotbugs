package jsr305;

import javax.annotation.Tainted;
import javax.annotation.Untainted;
import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class TaintedTest {
    @Untainted
    Object sanitize(@Untainted(when = When.UNKNOWN) Object o) {
        return o;
    }

    @NoWarning("TQ")
    void correctDoNotReport(@Tainted Object b) {
        Object x = sanitize(b);
        requiresUntainted(x);
    }

    @ExpectWarning("TQ")
    void violationReport(@Tainted Object a) {
        Object y = a;
        requiresUntainted(y);
    }

    protected abstract void requiresUntainted(@Untainted Object o);
}
