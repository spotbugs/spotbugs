package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PrintingBugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FindNonShortCircuitTest {
    private AnalysisContext ctx;
    private FindNonShortCircuit check;

    @Before
    public void setup() {
        PrintingBugReporter bugReporter = new PrintingBugReporter();
        ctx = AnalysisContext.currentAnalysisContext();
        if (ctx == null) {
            AnalysisContext.setCurrentAnalysisContext(new AnalysisContext(new Project()));
        }
        check = new FindNonShortCircuit(bugReporter);
    }

    @After
    public void tearDown() {
        AnalysisContext.setCurrentAnalysisContext(ctx);
    }

    @Test
    public void testDefaultBugTypeAndPriority() {
        BugInstance bug = check.createBugInstance();
        assertEquals(FindNonShortCircuit.NS_NON_SHORT_CIRCUIT, bug.getType());
        assertEquals(Priorities.LOW_PRIORITY, bug.getPriority());
    }

    @Test
    public void testBugTypeAndPriorityDangerOld() {
        check.sawDangerOld = true;
        BugInstance bug = check.createBugInstance();
        assertEquals(FindNonShortCircuit.NS_NON_SHORT_CIRCUIT, bug.getType());
        assertEquals(Priorities.NORMAL_PRIORITY, bug.getPriority());
    }

    @Test
    public void testBugTypeAndPriorityNullTestOld() {
        check.sawDangerOld = true;
        check.sawNullTestVeryOld = true;
        BugInstance bug = check.createBugInstance();
        assertEquals(FindNonShortCircuit.NS_NON_SHORT_CIRCUIT, bug.getType());
        assertEquals(Priorities.HIGH_PRIORITY, bug.getPriority());
    }

    @Test
    public void testBugTypeAndPriorityMethodCallOld() {
        check.sawDangerOld = true;
        check.sawMethodCallOld = true;
        BugInstance bug = check.createBugInstance();
        assertEquals(FindNonShortCircuit.NS_DANGEROUS_NON_SHORT_CIRCUIT, bug.getType());
        assertEquals(Priorities.HIGH_PRIORITY, bug.getPriority());
    }
}
