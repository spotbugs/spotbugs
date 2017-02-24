package jsr305;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class TestExhaustiveQualifier {
    @ExhaustiveQualifier(value = ExhaustiveQualifier.Color.RED, when = When.ALWAYS)
    Object redField;

    @ExhaustiveQualifier(value = ExhaustiveQualifier.Color.RED, when = When.NEVER)
    Object neverRedField;

    @ExhaustiveQualifier(value = ExhaustiveQualifier.Color.BLUE, when = When.ALWAYS)
    Object blueField;

    @ExpectWarning("TQ")
    public void report1(@ExhaustiveQualifier(value = ExhaustiveQualifier.Color.BLUE, when = When.ALWAYS) Object v) {
        // always BLUE should imply never RED
        redField = v;
    }

    @ExpectWarning("TQ")
    public void report1a(@AlwaysBlue Object v) {
        // always BLUE should imply never RED
        redField = v;
    }

    @NoWarning("TQ")
    public void noReport(@NeverBlue @NeverGreen Object v) {
        // no report: never blue and never green should imply always red
        redField = v;
    }

    @ExpectWarning("TQ")
    public void report2(@NeverBlue @NeverGreen Object v) {
        // report: never blue and never green should imply always red
        neverRedField = v;
    }

    @ExpectWarning("TQ")
    public void report3(@NeverBlue Object v) {
        // Sanity check - should see a warning here
        blueField = v;
    }

    @ExpectWarning("TQ")
    public void report4(@ExhaustiveQualifier(value = ExhaustiveQualifier.Color.BLUE, when = When.NEVER) Object v) {
        // Sanity check - should see a warning here
        blueField = v;
    }
}
