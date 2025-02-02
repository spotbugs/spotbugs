package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090205Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_02_05.class");
        // FNs in isDecember in line 24, 28
        // FNs in isBefore2009 (line 12), is2009 (line 16), is2008 (line 20), but it's difficult to know the programmer's intent,
        //  also the used function is already deprecated because of its confusing design

        assertBugInMethod("DMI_BAD_MONTH", "Ideas_2009_02_05", "endDate");
    }
}
