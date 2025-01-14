package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SharedVariableAtomicityDetectorTest extends AbstractIntegrationTest {
    private static final String WRITE_64BIT_BUG = "AT_NONATOMIC_64BIT_PRIMITIVE";
    private static final String PRIMITIVE_BUG = "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";
    private static final String OPS_BUG = "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE";

    @Test
    void bugForStalePrimitiveWriteWhenOtherMethodHasSynchronizedBlock() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlockAndBadVisibilityOnField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "SynchronizedBlockAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void bugForStalePrimitiveWriteWhenMethodHasIrrelevantSynchronizedBlock() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlockWithBadVisibilityOnField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "SynchronizedBlockWithBadVisibilityOnField", "shutdown", 20);
    }

    @Test
    void bugForStalePrimitiveWriteWhenOtherMethodIsSynchronized() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedMethodAndBadVisibilityOnField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "SynchronizedMethodAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    void bugForStalePrimitiveWriteWhenSetAndGetAreReordered() {
        performAnalysis("multithreaded/primitivewrite/FieldWithBadVisibilityReordered.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityReordered", "shutdown", 7);
    }

    @Test
    void bugForStalePrimitiveWriteWhenClassHasTwoSetters() {
        performAnalysis("multithreaded/primitivewrite/FieldWithBadVisibilityTwoSetters.class");
        assertBugTypeCount(PRIMITIVE_BUG, 2);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityTwoSetters", "shutdown", 18);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityTwoSetters", "up", 22);
    }

    @Test
    void bugForStalePrimitiveWriteWhenClassExtendsThread() {
        performAnalysis("multithreaded/primitivewrite/FieldWithBadVisibilityThread.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityThread", "shutdown", 18);
    }

    @Test
    void bugForStalePrimitiveWriteWhenClassImplementsRunnable() {
        performAnalysis("multithreaded/primitivewrite/FieldWithBadVisibilityRunnable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "FieldWithBadVisibilityRunnable", "shutdown", 18);
    }

    @Test
    void bugForStalePrimitiveWriteWhenHavingSeparateMethods() {
        performAnalysis("multithreaded/primitivewrite/NonsynchronizedSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "NonsynchronizedSeparateMethod", "shutdown", 23);
    }

    @Test
    void noBugWhenHavingAllCallsSynced() {
        performAnalysis("multithreaded/primitivewrite/AllCallSynchronizedSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugAtomicField() {
        performAnalysis("multithreaded/primitivewrite/AtomicField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSimpleVolatileField() {
        performAnalysis("multithreaded/primitivewrite/VolatileField.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlock() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlock.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedMethod() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedSeparateMethod() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlockSeparateMethod() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlockSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedLong() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedLong.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugVolatileLong() {
        performAnalysis("multithreaded/primitivewrite/VolatileLong.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlockDouble() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlockDouble.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugSynchronizedBlockDoubleFromOtherMethod() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlockDoubleFromOtherMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void reportFor64bitWriteNotAllReadSyncedOuter() {
        performAnalysis("multithreaded/primitivewrite/NotAllUsageSynchronizedDoubleOuter.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 1);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(WRITE_64BIT_BUG, "NotAllUsageSynchronizedDoubleOuter", "setValue", 7);
    }

    @Test
    void reportFor64bitWriteNotSyncedWrite() {
        performAnalysis("multithreaded/primitivewrite/NonSynchronizedWriteLong.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 1);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(WRITE_64BIT_BUG, "NonSynchronizedWriteLong", "setValue", 7);
    }

    @Test
    void reportFor64bitWriteNotAllSyncedWrite() {
        performAnalysis("multithreaded/primitivewrite/NotAllSynchronizedWriteLong.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 1);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(WRITE_64BIT_BUG, "NotAllSynchronizedWriteLong", "setValue", 7);
    }

    @Test
    void reportFor64bitWriteNotSynced() {
        performAnalysis("multithreaded/primitivewrite/NotSynchronizedLong.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 1);
        assertBugTypeCount(OPS_BUG, 0);
        assertBugInMethodAtLine(WRITE_64BIT_BUG, "NotSynchronizedLong", "setValue", 7);
    }

    @Test
    void noBugSynchronizedBlockPrimitiveSeparateMethod() {
        performAnalysis("multithreaded/primitivewrite/SynchronizedBlockPrimitiveSeparateMethod.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOpOnAtomicVariable() {
        performAnalysis("multithreaded/compoundoperation/CompoundOperationOnSharedAtomicVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationVolatileReadSyncWrite() {
        performAnalysis("multithreaded/compoundoperation/CompoundAdditionOnSharedVolatileReadSyncWrite.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationReadWriteLock() {
        performAnalysis("multithreaded/compoundoperation/CompoundNegateReadWriteLock.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationOnNotSharedVariable() {
        performAnalysis("multithreaded/compoundoperation/CompoundDivisionOnVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationInSynchronizedBlock() {
        performAnalysis("multithreaded/compoundoperation/SynchronizedBlockCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugCompoundOperationInSynchronizedMethod() {
        performAnalysis("multithreaded/compoundoperation/SynchronizedMethodCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    @Test
    void noBugNotRealDependentOp() {
        performAnalysis("multithreaded/compoundoperation/NotRealCompoundOp.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 0);
    }

    // --num
    @Test
    void bugForCompoundPreDecrementation() {
        performAnalysis("multithreaded/compoundoperation/CompoundPreDecrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPreDecrementationOnSharedVariable", "toggle", 7);
    }

    // num--
    @Test
    void bugForCompoundPostDecrementation() {
        performAnalysis("multithreaded/compoundoperation/CompoundPostDecrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPostDecrementationOnSharedVariable", "toggle", 7);
    }

    // ++num
    @Test
    void bugForCompoundPreIncrementation() {
        performAnalysis("multithreaded/compoundoperation/CompoundPreIncrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPreIncrementationOnSharedVariable", "toggle", 7);
    }

    // num++
    @Test
    void bugForCompoundPostIncrementation() {
        // The order of the functions is reversed
        performAnalysis("multithreaded/compoundoperation/CompoundPostIncrementationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundPostIncrementationOnSharedVariable", "toggle", 11);
    }

    // &=
    @Test
    void bugForCompoundIAND() {
        // considered multithreaded because it has a volatile field (not the problematic)
        performAnalysis("multithreaded/compoundoperation/CompoundIANDOperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundIANDOperationOnSharedVariable", "toggle", 8);
    }

    // |=
    @Test
    void bugForCompoundIOR() {
        // considered multithreaded because it has a field (not the problematic) from the java.util.concurrent.atomic package
        performAnalysis("multithreaded/compoundoperation/CompoundIOROperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundIOROperationOnSharedVariable", "toggle", 10);
    }

    // >>>=
    @Test
    void bugForCompoundLogicalRightShift() {
        // considered multithreaded because it extends Thread
        performAnalysis("multithreaded/compoundoperation/CompoundLogicalRightShiftingOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundLogicalRightShiftingOnSharedVariable", "toggle", 7);
    }

    // >>=
    @Test
    void bugForCompoundRightShift() {
        // considered multithreaded because it has a method with synchronized block
        performAnalysis("multithreaded/compoundoperation/CompoundRightShiftingOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundRightShiftingOnSharedVariable", "toggle", 7);
    }

    // <<=
    @Test
    void bugForCompoundLeftShift() {
        // considered multithreaded because it has synchronized method
        performAnalysis("multithreaded/compoundoperation/CompoundLeftShiftingOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundLeftShiftingOnSharedVariable", "toggle", 7);
    }

    // %=
    @Test
    void bugForCompoundModulo() {
        // considered multithreaded because it implements Runnable
        performAnalysis("multithreaded/compoundoperation/CompoundModuloOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundModuloOnSharedVariable", "run", 8);
    }

    // *=
    @Test
    void bugForCompoundMultiplication() {
        performAnalysis("multithreaded/compoundoperation/CompoundMultiplicationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundMultiplicationOnSharedVariable", "toggle", 7);
    }

    // /=
    @Test
    void bugForCompoundDivision() {
        performAnalysis("multithreaded/compoundoperation/CompoundDivisionOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundDivisionOnSharedVariable", "toggle", 7);
    }

    // -=
    @Test
    void bugForCompoundSubtraction() {
        performAnalysis("multithreaded/compoundoperation/CompoundSubtractionOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubtractionOnSharedVariable", "toggle", 7);
    }


    // +=
    @Test
    void bugForCompoundAdditionOnVolatileVar() {
        // simply defining the field as volatile is not enough
        performAnalysis("multithreaded/compoundoperation/CompoundAdditionOnSharedVolatileVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundAdditionOnSharedVolatileVariable", "toggle", 7);
    }

    // ^=
    @Test
    void bugForCompoundXOR() {
        // considered multithreaded because it has a field (not the problematic) with synchronized assignment
        performAnalysis("multithreaded/compoundoperation/CompoundXOROperationOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundXOROperationOnSharedVariable", "toggle", 12);
    }

    // num = num + 2
    @Test
    void bugForSimpleAdditionDependingOnPrevValue() {
        performAnalysis("multithreaded/compoundoperation/AdditionOnSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "AdditionOnSharedVariable", "toggle", 7);
    }

    // num = -num
    @Test
    void bugForNegateDependingOnPrevValue() {
        performAnalysis("multithreaded/compoundoperation/NegateSharedVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "NegateSharedVariable", "toggle", 7);
    }

    // num -= num + 2
    @Test
    void bugForCompoundSubtractionComplex() {
        performAnalysis("multithreaded/compoundoperation/CompoundSubstractComplexExpression.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubstractComplexExpression", "toggle", 7);
    }

    // num += num2 + 5
    @Test
    void bugForCompoundAdditionComplexWithAnotherVar() {
        performAnalysis("multithreaded/compoundoperation/CompoundAdditionComplexExpressionWithAnotherVar.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundAdditionComplexExpressionWithAnotherVar", "toggle", 8);
    }

    // num2 = num; num += 1
    @Test
    void bugForCompoundAdditionWithAnotherVar() {
        performAnalysis("multithreaded/compoundoperation/CompoundAdditionWithAnotherVar.class");
        assertBugTypeCount(PRIMITIVE_BUG, 1);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(PRIMITIVE_BUG, "CompoundAdditionWithAnotherVar", "toggle", 8);
        assertBugInMethodAtLine(OPS_BUG, "CompoundAdditionWithAnotherVar", "toggle", 9);
    }

    // num += param
    @Test
    void bugForCompoundSubstractionOfArg() {
        performAnalysis("multithreaded/compoundoperation/CompoundSubstractionOfArg.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubstractionOfArg", "toggle", 7);
    }

    // num += getOne()
    @Test
    void bugForCompoundSubstractionOfFunCall() {
        performAnalysis("multithreaded/compoundoperation/CompoundSubstractionOfMethodReturnValue.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 1);
        assertBugInMethodAtLine(OPS_BUG, "CompoundSubstractionOfMethodReturnValue", "toggle", 7);
    }

    @Test
    void bugForTwoCompoundOperations() {
        performAnalysis("multithreaded/compoundoperation/CompoundDivideMultiplyOnVariable.class");
        assertBugTypeCount(PRIMITIVE_BUG, 0);
        assertBugTypeCount(WRITE_64BIT_BUG, 0);
        assertBugTypeCount(OPS_BUG, 2);
        assertBugInMethodAtLine(OPS_BUG, "CompoundDivideMultiplyOnVariable", "divide", 7);
        assertBugInMethodAtLine(OPS_BUG, "CompoundDivideMultiplyOnVariable", "multiply", 11);
    }
}
