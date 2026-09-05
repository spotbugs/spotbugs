package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PriorityAdjuster.adjustPriority behavior.
 */
class PriorityAdjusterTest {
    private DetectorFactory detFactory;
    private Detector2 findNullDerefDetector;
    private Project project;

    @Test
    void returnsOriginalInstanceWhenNoAdjustments() {
        PriorityAdjuster adjuster = new PriorityAdjuster(Map.of());
        BugInstance bug = new BugInstance(findNullDerefDetector, "NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        int priority = bug.getPriority();
        BugInstance result = adjuster.adjustPriority(bug);

        assertSame(bug, result);
        assertEquals(priority, result.getPriority());
    }

    @BeforeEach
    void setUp() {
        detFactory = DetectorFactoryCollection.instance().getFactory("FindNullDeref");
        project = new Project();
        AnalysisContext analysisContext = new AnalysisContext(project) {
            public boolean isApplicationClass(@DottedClassName String className) {
                // treat all classes as application class, to report bugs in it
                return true;
            }
        };
        AnalysisContext.setCurrentAnalysisContext(analysisContext);
        findNullDerefDetector = detFactory.createDetector2(new BugCollectionBugReporter(project));
    }

    @AfterEach
    void tearDown() {
        project.close();
        AnalysisContext.removeCurrentAnalysisContext();
        AnalysisContext.setCurrentAnalysisContext(null);
    }

    @Test
    void raiseBasedOnBugPattern() {
        PriorityAdjuster adjuster = new PriorityAdjuster(Map.of("NP_NULL_ON_SOME_PATH", "raise"));
        BugInstance bug = new BugInstance(findNullDerefDetector, "NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        int priority = bug.getPriority();
        BugInstance result = adjuster.adjustPriority(bug);

        assertNotSame(bug, result);
        assertEquals(priority - 1, result.getPriority());
    }

    @Test
    void setBasedOnDetectorFQCN() {
        PriorityAdjuster adjuster = new PriorityAdjuster(Map.of("edu.umd.cs.findbugs.detect.FindNullDeref", "1"));
        BugInstance bug = new BugInstance(findNullDerefDetector, "NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        int priority = bug.getPriority();
        BugInstance result = adjuster.adjustPriority(bug);

        assertNotSame(bug, result);
        assertEquals(1, result.getPriority());
    }

    @Test
    void lowerBasedOnDetectorSimpleClassName() {
        PriorityAdjuster adjuster = new PriorityAdjuster(Map.of("FindNullDeref", "+1"));
        BugInstance bug = new BugInstance(findNullDerefDetector, "NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        int priority = bug.getPriority();
        BugInstance result = adjuster.adjustPriority(bug);

        assertNotSame(bug, result);
        assertEquals(priority + 1, result.getPriority());
    }

    @Test
    void lowerAndRaise() {
        PriorityAdjuster adjuster = new PriorityAdjuster(Map.of("FindNullDeref", "+1", "NP_NULL_ON_SOME_PATH", "-1"));
        BugInstance bug = new BugInstance(findNullDerefDetector, "NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        int priority = bug.getPriority();
        BugInstance result = adjuster.adjustPriority(bug);

        assertSame(bug, result);
        assertEquals(priority, result.getPriority());
    }

    @Test
    void patternAdjustmentWinsOverDetector() {
        PriorityAdjuster adjuster = new PriorityAdjuster(Map.of("FindNullDeref", "+1", "NP_NULL_ON_SOME_PATH", "1"));
        BugInstance bug = new BugInstance(findNullDerefDetector, "NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        BugInstance result = adjuster.adjustPriority(bug);

        assertNotSame(bug, result);
        assertEquals(1, result.getPriority());
    }

    @Test
    void testBadKey() {
        new PriorityAdjuster(Map.of("RC_REF_COMPARISON", "suppress")); // should be ok
        assertThrows(IllegalArgumentException.class, () -> {
            new PriorityAdjuster(Map.of("something", "suppress"));
        });
    }

    @Test
    void testBadValue() {
        new PriorityAdjuster(Map.of("RC_REF_COMPARISON", "suppress")); // should be ok
        assertThrows(IllegalArgumentException.class, () -> {
            new PriorityAdjuster(Map.of("RC_REF_COMPARISON", "something"));
        });
    }
}
