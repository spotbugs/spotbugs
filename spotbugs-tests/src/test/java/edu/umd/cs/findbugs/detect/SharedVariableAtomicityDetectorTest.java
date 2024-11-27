package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SharedVariableAtomicityDetectorTest extends AbstractIntegrationTest {
    private static final String WRITE_64BIT_BUG_TYPE = "AT_NONATOMIC_64BIT_PRIMITIVE";
    private static final String VISIBILITY_BUG_TYPE = "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";
    private static final String OPS_BUG_TYPE = "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE";

    @Test
    void failurePath_fieldWithBadVisibility_whenOtherMethodHasSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockAndBadVisibilityOnField.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "SynchronizedBlockAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenMethodHasIrrelevantSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockWithBadVisibilityOnField.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "SynchronizedBlockWithBadVisibilityOnField", "shutdown", 20);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenOtherMethodIsSynchronized() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethodAndBadVisibilityOnField.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "SynchronizedMethodAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenSetAndGetAreReordered() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityReordered.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "FieldWithBadVisibilityReordered", "shutdown", 7);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassHasTwoSetters() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityTwoSetters.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 2);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "FieldWithBadVisibilityTwoSetters", "shutdown", 18);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "FieldWithBadVisibilityTwoSetters", "up", 22);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassExtendsThread() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityThread.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "FieldWithBadVisibilityThread", "shutdown", 18);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenClassImplementsRunnable() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityRunnable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "FieldWithBadVisibilityRunnable", "shutdown", 18);
    }

    @Test
    void failurePath_fieldWithBadVisibility_whenHavingSeparateMethods() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/NonsynchronizedSeparateMethod.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "NonsynchronizedSeparateMethod", "shutdown", 22);
    }

    @Test
    void happyPath_atomicField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/AtomicField.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_volatileField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/VolatileField.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlock.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethod.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedSeparateMethod.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_synchronizedBlockSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockSeparateMethod.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }


    @Test
    void happyPath_compoundOperation_onSharedAtomicVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundOperationOnSharedAtomicVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperation_onSharedVariable_volatileReadSyncWrite() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileReadSyncWrite.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperation_readWriteLock() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundNegateReadWriteLock.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperation_onNotSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperationInsideSynchronizedBlock_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedBlockCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperationInsideSynchronizedMethod_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedMethodCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 0);
    }

    // --num
    @Test
    void reportsBugFor_compoundPreDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreDecrementationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundPreDecrementationOnSharedVariable", "toggle", 7);
    }

    // num--
    @Test
    void reportsBugFor_compoundPostDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostDecrementationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundPostDecrementationOnSharedVariable", "toggle", 7);
    }

    // ++num
    @Test
    void reportsBugFor_compoundPreIncrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreIncrementationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundPreIncrementationOnSharedVariable", "toggle", 7);
    }

    // num++
    @Test
    void reportsBugFor_compoundPostIncrementation_onSharedVariable() {
        // The order of the functions is reversed
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostIncrementationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundPostIncrementationOnSharedVariable", "toggle", 11);
    }

    // &=
    @Test
    void reportsBugFor_compoundIAND_onSharedVariable() {
        // considered multithreaded because it has a volatile field (not the problematic)
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIANDOperationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundIANDOperationOnSharedVariable", "toggle", 8);
    }

    // |=
    @Test
    void reportsBugFor_compoundIOR_onSharedVariable() {
        // considered multithreaded because it has a field (not the problematic) from the java.util.concurrent.atomic package
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIOROperationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundIOROperationOnSharedVariable", "toggle", 10);
    }

    // >>>=
    @Test
    void reportsBugFor_compoundLogicalRightShifting_onSharedVariable() {
        // considered multithreaded because it extends Thread
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLogicalRightShiftingOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundLogicalRightShiftingOnSharedVariable", "toggle", 7);
    }

    // >>=
    @Test
    void reportsBugFor_compoundRightShifting_onSharedVariable() {
        // considered multithreaded because it has a method with synchronized block
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundRightShiftingOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundRightShiftingOnSharedVariable", "toggle", 7);
    }

    // <<=
    @Test
    void reportsBugFor_compoundLeftShifting_onSharedVariable() {
        // considered multithreaded because it has synchronized method
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLeftShiftingOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundLeftShiftingOnSharedVariable", "toggle", 7);
    }

    // %=
    @Test
    void reportsBugFor_compoundModulo_onSharedVariable() {
        // considered multithreaded because it implements Runnable
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundModuloOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundModuloOnSharedVariable", "run", 8);
    }

    // *=
    @Test
    void reportsBugFor_compoundMultiplication_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundMultiplicationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundMultiplicationOnSharedVariable", "toggle", 7);
    }

    // /=
    @Test
    void reportsBugFor_compoundDivision_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundDivisionOnSharedVariable", "toggle", 7);
    }

    // -=
    @Test
    void reportsBugFor_compoundSubtraction_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubtractionOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundSubtractionOnSharedVariable", "toggle", 7);
    }


    // +=
    @Test
    void reportsBugFor_compoundAddition_onSharedVolatileVariable() {
        // simply defining the field as volatile is not enough
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundAdditionOnSharedVolatileVariable", "toggle", 7);
    }

    // ^=
    @Test
    void reportsBugFor_compoundXOROperation_onSharedVariable() {
        // considered multithreaded because it has a field (not the problematic) with synchronized assignment
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundXOROperationOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundXOROperationOnSharedVariable", "toggle", 12);
    }

    // num = num + 2
    @Test
    void reportsBugFor_simpleAddition_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/AdditionOnSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "AdditionOnSharedVariable", "toggle", 7);
    }

    // num = -num
    @Test
    void reportsBugFor_negate_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/NegateSharedVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "NegateSharedVariable", "toggle", 7);
    }

    // num -= num + 2
    @Test
    void reportsBugFor_compoundSubtraction_onSharedVariable_complex() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractComplexExpression.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundSubstractComplexExpression", "toggle", 7);
    }

    // num += num2 + 5
    @Test
    void reportsBugFor_compoundAddition_onSharedVariable_complex_withAnotherVar() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionComplexExpressionWithAnotherVar.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundAdditionComplexExpressionWithAnotherVar", "toggle", 8);
    }

    // num2 = num; num += 1
    @Test
    void reportsBugFor_compoundAddition_onSharedVariable_withAnotherVar() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionWithAnotherVar.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 1);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(VISIBILITY_BUG_TYPE, "CompoundAdditionWithAnotherVar", "toggle", 8);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundAdditionWithAnotherVar", "toggle", 9);
    }

    // num += param
    @Test
    void reportsBugFor_compoundSubstractionOfArg_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractionOfArg.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundSubstractionOfArg", "toggle", 7);
    }

    // num += getOne()
    @Test
    void reportsBugFor_compoundSubstractionOfFunCall_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractionOfMethodReturnValue.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 1);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundSubstractionOfMethodReturnValue", "toggle", 7);
    }

    @Test
    void reportsBugFor_twoCompoundOperations_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivideMultiplyOnVariable.class");
        assertBugTypeCount(VISIBILITY_BUG_TYPE, 0);
        assertBugTypeCount(WRITE_64BIT_BUG_TYPE, 0);
        assertBugTypeCount(OPS_BUG_TYPE, 2);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundDivideMultiplyOnVariable", "divide", 7);
        assertBugInMethodAtLine(OPS_BUG_TYPE, "CompoundDivideMultiplyOnVariable", "multiply", 11);
    }
}
