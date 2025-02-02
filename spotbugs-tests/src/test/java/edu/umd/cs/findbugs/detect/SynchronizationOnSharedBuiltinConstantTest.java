package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class SynchronizationOnSharedBuiltinConstantTest extends AbstractIntegrationTest {

    @Test
    void lockOn_noncompliantBooleanLockObject() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        assertBugInMethod("DL_SYNCHRONIZATION_ON_BOOLEAN", "SynchronizationOnSharedBuiltinConstantBad", "noncompliantBooleanLockObject");
    }

    @Test
    void lockOn_noncompliantBoxedPrimitive() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        assertBugInMethod("DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE", "SynchronizationOnSharedBuiltinConstantBad", "noncompliantBoxedPrimitive");
    }

    @Test
    void lockOn_compliantInteger() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantGood.class");
        assertNoBugInMethod("DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE", "SynchronizationOnSharedBuiltinConstantGood", "compliantInteger");
    }

    @Test
    void lockOn_noncompliantInternedStringObject() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        assertBugInMethod("DL_SYNCHRONIZATION_ON_INTERNED_STRING", "SynchronizationOnSharedBuiltinConstantBad", "noncompliantInternedStringObject");
    }

    @Test
    void lockOn_noncompliantStringLiteral() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        assertBugInMethod("DL_SYNCHRONIZATION_ON_SHARED_CONSTANT", "SynchronizationOnSharedBuiltinConstantBad", "noncompliantStringLiteral");
    }

    @Test
    void lockOn_compliantStringInstance() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantGood.class");
        assertNoBugInMethod("DL_SYNCHRONIZATION_ON_SHARED_CONSTANT", "SynchronizationOnSharedBuiltinConstantGood", "compliantStringInstance");
    }

    @Test
    void lockOn_compliantPrivateFinalLockObject() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantGood.class");
        assertNoBugInMethod("DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE", "SynchronizationOnSharedBuiltinConstantGood", "compliantPrivateFinalLockObject");
    }

}
