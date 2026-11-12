package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class Issue3196Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testRecordPatterns() {
        performAnalysis(
                "../java21/Issue3196.class",
                "../java21/Issue3196$One.class",
                "../java21/Issue3196$Two.class",
                "../java21/Issue3196$Foo.class",
                "../java21/Issue3196$Holder.class");

        assertNoBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue3196", "nestedSwitch");
        assertNoBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue3196", "topLevel");
        assertNoBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue3196", "nestedIf");
        assertBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue3196", "handWrittenNested");
        assertBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue3196", "handWrittenTopLevel");
        assertBugTypeCount("BC_VACUOUS_INSTANCEOF", 2);
    }
}
