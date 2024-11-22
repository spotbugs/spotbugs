package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class BadVisibilityOnSharedPrimitiveVariablesTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES";

    @Test
    void failurePath_fieldWithBadVisibility_whenOtherMethodHasSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockAndBadVisibilityOnField.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "SynchronizedBlockAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenMethodHasIrrelevantSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockWithBadVisibilityOnField.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "SynchronizedBlockWithBadVisibilityOnField", "shutdown", 20);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenOtherMethodIsSynchronized() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethodAndBadVisibilityOnField.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "SynchronizedMethodAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenSetAndGetAreReordered() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityReordered.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityReordered", "run", 12);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassHasTwoSetters() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityTwoSetters.class");
        assertBugTypeCount(BUG_TYPE, 2);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityTwoSetters", "shutdown", 18);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityTwoSetters", "up", 22);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassExtendsThread() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityThread.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityThread", "shutdown", 18);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassImplementsRunnable() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityRunnable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "FieldWithBadVisibilityRunnable", "shutdown", 18);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenHavingSeparateMethods() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/NonsynchronizedSeparateMethod.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "NonsynchronizedSeparateMethod", "shutdown", 22);
    }

    @Test
    void happyPath_atomicField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/AtomicField.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_volatileField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/VolatileField.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlock.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethod.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedSeparateMethod.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedBlockSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockSeparateMethod.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }
}
