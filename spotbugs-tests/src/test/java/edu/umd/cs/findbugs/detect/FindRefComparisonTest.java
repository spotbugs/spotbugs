package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.Confidence;

class FindRefComparisonTest extends AbstractIntegrationTest {

    @AfterAll
    static void tearDown() {
        SystemProperties.removeProperty("findbugs.refcomp.reportAll");
    }

    private void performAnalysisForRefComp() {
        performAnalysis("RefComparisons.class", "RefComparisons$MyClass1.class", "RefComparisons$MyClass2.class", "RefComparisons$MyClass3.class",
                "RefComparisons$MyEnum.class");
    }

    @Test
    void testComparisonsNotEnabled() {
        // When there is no priority adjustment, and we enable ref comparison for
        // all, we only get an integer ref comparison bug.
        SystemProperties.setProperty("findbugs.refcomp.reportAll", "true"); // enable ref comparison

        performAnalysisForRefComp();
        assertBugTypeCount("RC_REF_COMPARISON", 1);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "integerBadComparison", 5,
                Confidence.HIGH);
    }

    @Test
    void testComparisonsWhenPropertyUnset() {
        // When raising the priority, we get all ref comparison bugs.
        setPriorityAdjustment(-1); // raise priority

        SystemProperties.removeProperty("findbugs.refcomp.reportAll");
        performAnalysisForRefComp();
        assertBugTypeCount("RC_REF_COMPARISON", 3);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "integerBadComparison", 5, Confidence.HIGH);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "myClass1BadComparison", 13, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "myClass2BadComparison", 21, Confidence.LOW);
    }

    @Test
    void testComparisonsWhenReportAllIsNotEnabled() {
        setPriorityAdjustment(-1); // raise priority
        SystemProperties.setProperty("findbugs.refcomp.reportAll", "false");

        performAnalysis("RefComparisons.class", "RefComparisons$MyEnum.class", "RefComparisons$MyClass2.class",
                "RefComparisons$MyClass1.class", "RefComparisons$MyClass3.class");
        assertBugTypeCount("RC_REF_COMPARISON", 1);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "integerBadComparison", 5, Confidence.HIGH);
    }

    @Test
    void testComparisonsWhenReportAllIsEnabled() {
        setPriorityAdjustment(-1); // raise priority
        SystemProperties.setProperty("findbugs.refcomp.reportAll", "true");

        performAnalysisForRefComp();
        assertBugTypeCount("RC_REF_COMPARISON", 3);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "integerBadComparison", 5, Confidence.HIGH);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "myClass1BadComparison", 13, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("RC_REF_COMPARISON", "RefComparisons", "myClass2BadComparison", 21, Confidence.LOW);
    }

    private static void setPriorityAdjustment(int adjustment) {
        DetectorFactory detFactory = DetectorFactoryCollection.instance().getFactory(FindRefComparison.class.getSimpleName());
        detFactory.setPriorityAdjustment(adjustment);
    }

}
