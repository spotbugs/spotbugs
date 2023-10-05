package edu.umd.cs.findbugs;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the logic in the reportBug method in all its permutations.
 */
public class AbstractBugReporterTest {
    private BugInstance bugInstance;
    private ObservableBugReporter reporter;

    @Before
    public void setup() {
        // BugRank = 8, Priority = 2
        bugInstance = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        reporter = new ObservableBugReporter();

        Project project = new Project();
        AnalysisContext analysisContext = new AnalysisContext(project) {
            public boolean isApplicationClass(@DottedClassName String className) {
                // treat all classes as application class, to report bugs in it
                return true;
            }
        };
        AnalysisContext.setCurrentAnalysisContext(analysisContext);
    }

    @After
    public void teardown() {
        AnalysisContext.removeCurrentAnalysisContext();
    }

    /**
     * Function to do the actual test and make assertions.
     *
     * @param expectedReport true/false depending on expected outcome
     * @param message human-readable message
     * @param rankThreshold the rank threshold
     * @param priorityThreshold the priority threshold
     */
    private void assertReported(boolean expectedReport, String message, int rankThreshold, int priorityThreshold) {
        reporter.reset();
        reporter.setPriorityThreshold(priorityThreshold);
        reporter.setRankThreshold(rankThreshold);

        reporter.reportBug(bugInstance);
        Assert.assertEquals(message, expectedReport, reporter.isReportCalled());
    }

    @Test
    public void verifyPriorityThresholdWorks() {
        assertReported(false, "Normal bug report blocked on high",
                BugRanker.VISIBLE_RANK_MAX, Priorities.HIGH_PRIORITY);

        assertReported(true, "Normal bug report reported on normal",
                BugRanker.VISIBLE_RANK_MAX, Priorities.NORMAL_PRIORITY);

        assertReported(true, "Normal bug report reported on low",
                BugRanker.VISIBLE_RANK_MAX, Priorities.LOW_PRIORITY);
    }

    @Test
    public void verifyRankThresholdWorks() {
        int bugRank = bugInstance.getBugRank();

        assertReported(false, "Rank " + bugRank + " bug report blocked on min visible",
                BugRanker.VISIBLE_RANK_MIN, Priorities.LOW_PRIORITY);

        assertReported(true, "Rank " + bugRank + " bug report works on equal",
                bugRank, Priorities.LOW_PRIORITY);

        assertReported(true, "Rank " + bugRank + " bug report works on max visible",
                BugRanker.VISIBLE_RANK_MAX, Priorities.LOW_PRIORITY);
    }

    @Test
    public void verifyRelaxedOperationWorks() {
        reporter.setIsRelaxed(true);
        assertReported(true, "Relaxed bug reported even when it would not",
                BugRanker.VISIBLE_RANK_MIN, Priorities.HIGH_PRIORITY);

        reporter.setIsRelaxed(false);
        assertReported(false, "No report when not relaxed and blocked",
                BugRanker.VISIBLE_RANK_MIN, Priorities.HIGH_PRIORITY);
    }

    private class ObservableBugReporter extends AbstractBugReporter {

        private boolean reportCalled;

        public boolean isReportCalled() {
            return reportCalled;
        }

        public void reset() {
            reportCalled = false;
        }

        @Override
        protected void doReportBug(BugInstance bugInstance) {
            reportCalled = true;
        }

        @Override
        public void reportAnalysisError(AnalysisError error) {
        }

        @Override
        public void reportMissingClass(String string) {
        }

        @Override
        public void finish() {
        }

        @CheckForNull
        @Override
        public BugCollection getBugCollection() {
            return null;
        }

        @Override
        public void observeClass(ClassDescriptor classDescriptor) {
        }
    }
}
