package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SharedVariableAtomicityDetectorTest extends AbstractIntegrationTest {
    private static final String WRITE_64BIT_BUG = "AT_NONATOMIC_64BIT_PRIMITIVE";
    private static final String PRIMITIVE_BUG = "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";
    private static final String OPS_BUG = "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE";

    @Test
    void bugForStalePrimitiveWriteWhenOtherMethodHasSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockAndBadVisibilityOnField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "SynchronizedBlockAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void bugForStalePrimitiveWriteWhenMethodHasIrrelevantSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockWithBadVisibilityOnField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "SynchronizedBlockWithBadVisibilityOnField", "shutdown", 20);
    }

    @Test
    void bugForStalePrimitiveWriteWhenOtherMethodIsSynchronized() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethodAndBadVisibilityOnField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "SynchronizedMethodAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void bugForStalePrimitiveWriteWhenSetAndGetAreReordered() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityReordered.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityReordered", "shutdown", 7);
    }

    @Test
    void bugForStalePrimitiveWriteWhenClassHasTwoSetters() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityTwoSetters.class");
        assertBugTypeCount(PRIMITIVE_BUG, 2);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityTwoSetters", "shutdown", 18);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityTwoSetters", "up", 22);
    }

    @Test
    void bugForStalePrimitiveWriteWhenClassExtendsThread() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityThread.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityThread", "shutdown", 18);
    }

    @Test
    void bugForStalePrimitiveWriteWhenClassImplementsRunnable() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityRunnable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityRunnable", "shutdown", 18);
    }

    @Test
    void bugForStalePrimitiveWriteWhenHavingSeparateMethods() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/NonsynchronizedSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "NonsynchronizedSeparateMethod", "shutdown", 22);
    }

    @Test
    void noBugAtomicField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/AtomicField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSimpleVolatileField() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/VolatileField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlock() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlock.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlockSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlockPrimitiveSeparateMethod() {
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockPrimitiveSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOpOnAtomicVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundOperationOnSharedAtomicVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationVolatileReadSyncWrite() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileReadSyncWrite.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationReadWriteLock() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundNegateReadWriteLock.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationOnNotSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationInSynchronizedBlock() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedBlockCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationInSynchronizedMethod() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedMethodCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    // --num
    @Test
    void bugForCompoundPreDecrementation() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreDecrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPreDecrementationOnSharedVariable", "toggle", 7);
    }

    // num--
    @Test
    void bugForCompoundPostDecrementation() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostDecrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPostDecrementationOnSharedVariable", "toggle", 7);
    }

    // ++num
    @Test
    void bugForCompoundPreIncrementation() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreIncrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPreIncrementationOnSharedVariable", "toggle", 7);
    }

    // num++
    @Test
    void bugForCompoundPostIncrementation() {
        // The order of the functions is reversed
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostIncrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPostIncrementationOnSharedVariable", "toggle", 11);
    }

    // &=
    @Test
    void bugForCompoundIAND() {
        // considered multithreaded because it has a volatile field (not the problematic)
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIANDOperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundIANDOperationOnSharedVariable", "toggle", 8);
    }

    // |=
    @Test
    void bugForCompoundIOR() {
        // considered multithreaded because it has a field (not the problematic) from the java.util.concurrent.atomic package
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIOROperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundIOROperationOnSharedVariable", "toggle", 10);
    }

    // >>>=
    @Test
    void bugForCompoundLogicalRightShift() {
        // considered multithreaded because it extends Thread
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLogicalRightShiftingOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundLogicalRightShiftingOnSharedVariable", "toggle", 7);
    }

    // >>=
    @Test
    void bugForCompoundRightShift() {
        // considered multithreaded because it has a method with synchronized block
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundRightShiftingOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundRightShiftingOnSharedVariable", "toggle", 7);
    }

    // <<=
    @Test
    void bugForCompoundLeftShift() {
        // considered multithreaded because it has synchronized method
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLeftShiftingOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundLeftShiftingOnSharedVariable", "toggle", 7);
    }

    // %=
    @Test
    void bugForCompoundModulo() {
        // considered multithreaded because it implements Runnable
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundModuloOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundModuloOnSharedVariable", "run", 8);
    }

    // *=
    @Test
    void bugForCompoundMultiplication() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundMultiplicationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundMultiplicationOnSharedVariable", "toggle", 7);
    }

    // /=
    @Test
    void bugForCompoundDivision() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundDivisionOnSharedVariable", "toggle", 7);
    }

    // -=
    @Test
    void bugForCompoundSubtraction() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubtractionOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubtractionOnSharedVariable", "toggle", 7);
    }


    // +=
    @Test
    void bugForCompoundAdditionOnVolatileVar() {
        // simply defining the field as volatile is not enough
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundAdditionOnSharedVolatileVariable", "toggle", 7);
    }

    // ^=
    @Test
    void bugForCompoundXOR() {
        // considered multithreaded because it has a field (not the problematic) with synchronized assignment
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundXOROperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundXOROperationOnSharedVariable", "toggle", 12);
    }

    // num = num + 2
    @Test
    void bugForSimpleAdditionDependingOnPrevValue() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/AdditionOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "AdditionOnSharedVariable", "toggle", 7);
    }

    // num = -num
    @Test
    void bugForNegateDependingOnPrevValue() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/NegateSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "NegateSharedVariable", "toggle", 7);
    }

    // num -= num + 2
    @Test
    void bugForCompoundSubtractionComplex() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractComplexExpression.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubstractComplexExpression", "toggle", 7);
    }

    // num += num2 + 5
    @Test
    void bugForCompoundAdditionComplexWithAnotherVar() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionComplexExpressionWithAnotherVar.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundAdditionComplexExpressionWithAnotherVar", "toggle", 8);
    }

    // num2 = num; num += 1
    @Test
    void bugForCompoundAdditionWithAnotherVar() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionWithAnotherVar.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "CompoundAdditionWithAnotherVar", "toggle", 8);
        assertBugInMethodAtLine(OPS_BUG, "CompoundAdditionWithAnotherVar", "toggle", 9);
    }

    // num += param
    @Test
    void bugForCompoundSubstractionOfArg() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractionOfArg.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubstractionOfArg", "toggle", 7);
    }

    // num += getOne()
    @Test
    void bugForCompoundSubstractionOfFunCall() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractionOfMethodReturnValue.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubstractionOfMethodReturnValue", "toggle", 7);
    }

    @Test
    void bugForTwoCompoundOperations() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivideMultiplyOnVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 2);
        assertBugInMethodAtLine(OPS_BUG, "CompoundDivideMultiplyOnVariable", "divide", 7);
        assertBugInMethodAtLine(OPS_BUG, "CompoundDivideMultiplyOnVariable", "multiply", 11);
    }
}
