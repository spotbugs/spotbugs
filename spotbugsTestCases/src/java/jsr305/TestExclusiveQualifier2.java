package jsr305;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestExclusiveQualifier2 {
    @NoWarning("TQ")
    @AlwaysRed
    Object redField;

    @NoWarning("TQ")
    @AlwaysRed
    Object getRed() {
        return redField;
    }

    @NoWarning("TQ")
    void setRed(@AlwaysRed Object redField) {
        this.redField = redField;
    }

    @ExpectWarning("TQ")
    public void report1(@AlwaysBlue Object v) {
        // always BLUE should imply never RED
        redField = v;
    }

    @ExpectWarning("TQ")
    public void report2(@AlwaysBlue Object b, @AlwaysRed Object r, boolean condition) {
        // always BLUE should imply never RED
        Object x;
        if (condition)
            x = b;
        else
            x = r;
        redField = x;
    }

    @ExpectWarning("TQ")
    public void report3(@AlwaysBlue Object b, @AlwaysRed Object r, boolean condition) {
        // always BLUE should imply never RED
        Object x;
        if (condition)
            x = r;
        else
            x = b;
        redField = x;
    }

    @NoWarning("TQ")
    public void doNotReport(Object b) {
        redField = b;
    }
}
