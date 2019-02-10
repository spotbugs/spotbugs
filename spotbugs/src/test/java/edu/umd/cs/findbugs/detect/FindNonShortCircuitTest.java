package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PrintingBugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import org.junit.Test;

import static org.junit.Assert.*;

public class FindNonShortCircuitTest {

    @Test
    public void testBugTypeAndPriority() {
        PrintingBugReporter bugReporter = new PrintingBugReporter();
        AnalysisContext ctx = AnalysisContext.currentAnalysisContext();
        try {
            if (ctx == null) {
                AnalysisContext.setCurrentAnalysisContext(new AnalysisContext(new Project()));
            }
            FindNonShortCircuit check = new FindNonShortCircuit(bugReporter);
            BugInstance bug = check.getBugInstance();
            assertEquals(FindNonShortCircuit.NS_NON_SHORT_CIRCUIT, bug.getType());
            assertEquals(Priorities.LOW_PRIORITY, bug.getPriority());

            check.sawDangerOld = true;
            bug = check.getBugInstance();
            assertEquals(FindNonShortCircuit.NS_NON_SHORT_CIRCUIT, bug.getType());
            assertEquals(Priorities.NORMAL_PRIORITY, bug.getPriority());

            check.sawNullTestVeryOld = true;
            bug = check.getBugInstance();
            assertEquals(FindNonShortCircuit.NS_NON_SHORT_CIRCUIT, bug.getType());
            assertEquals(Priorities.HIGH_PRIORITY, bug.getPriority());

            check.sawNullTestVeryOld = false;
            check.sawMethodCallOld = true;
            bug = check.getBugInstance();
            assertEquals(FindNonShortCircuit.NS_DANGEROUS_NON_SHORT_CIRCUIT, bug.getType());
            assertEquals(Priorities.HIGH_PRIORITY, bug.getPriority());
        } finally {
            AnalysisContext.setCurrentAnalysisContext(ctx);
        }
    }
}