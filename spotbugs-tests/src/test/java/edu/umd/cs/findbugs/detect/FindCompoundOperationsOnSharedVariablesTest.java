package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindCompoundOperationsOnSharedVariablesTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES";

    @Test
    void happyPath_compoundOperation_onSharedAtomicVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundOperationOnSharedAtomicVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperation_onSharedVariable_volatileReadSyncWrite() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileReadSyncWrite.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperation_readWriteLock() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundNegateReadWriteLock.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperation_onNotSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperationInsideSynchronizedBlock_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedBlockCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    @Test
    void happyPath_compoundOperationInsideSynchronizedMethod_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedMethodCompoundOperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 0);
    }

    // --num
    @Test
    void reportsBugFor_compoundPreDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreDecrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPreDecrementationOnSharedVariable", "getNum", 11);
    }

    // num--
    @Test
    void reportsBugFor_compoundPostDecrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostDecrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPostDecrementationOnSharedVariable", "getNum", 11);
    }

    // ++num
    @Test
    void reportsBugFor_compoundPreIncrementation_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreIncrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPreIncrementationOnSharedVariable", "getNum", 11);
    }

    // num++
    @Test
    void reportsBugFor_compoundPostIncrementation_onSharedVariable() {
        // The order of the functions is reversed
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostIncrementationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundPostIncrementationOnSharedVariable", "toggle", 11);
    }

    // &=
    @Test
    void reportsBugFor_compoundIAND_onSharedVariable() {
        // considered multithreaded because it has a volatile field (not the problematic)
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIANDOperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundIANDOperationOnSharedVariable", "getNum", 12);
    }

    // |=
    @Test
    void reportsBugFor_compoundIOR_onSharedVariable() {
        // considered multithreaded because it has a field (not the problematic) from the java.util.concurrent.atomic package
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIOROperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundIOROperationOnSharedVariable", "getNum", 14);
    }

    // >>>=
    @Test
    void reportsBugFor_compoundLogicalRightShifting_onSharedVariable() {
        // considered multithreaded because it extends Thread
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLogicalRightShiftingOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundLogicalRightShiftingOnSharedVariable", "getNum", 11);
    }

    // >>=
    @Test
    void reportsBugFor_compoundRightShifting_onSharedVariable() {
        // considered multithreaded because it has a method with synchronized block
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundRightShiftingOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundRightShiftingOnSharedVariable", "getNum", 11);
    }

    // <<=
    @Test
    void reportsBugFor_compoundLeftShifting_onSharedVariable() {
        // considered multithreaded because it has synchronized method
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLeftShiftingOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundLeftShiftingOnSharedVariable", "getNum", 11);
    }

    // %=
    @Test
    void reportsBugFor_compoundModulo_onSharedVariable() {
        // considered multithreaded because it implements Runnable
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundModuloOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundModuloOnSharedVariable", "getNum", 12);
    }

    // *=
    @Test
    void reportsBugFor_compoundMultiplication_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundMultiplicationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundMultiplicationOnSharedVariable", "getNum", 11);
    }

    // /=
    @Test
    void reportsBugFor_compoundDivision_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundDivisionOnSharedVariable", "getNum", 11);
    }

    // -=
    @Test
    void reportsBugFor_compoundSubtraction_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubtractionOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundSubtractionOnSharedVariable", "getNum", 11);
    }


    // +=
    @Test
    void reportsBugFor_compoundAddition_onSharedVolatileVariable() {
        // simply defining the field as volatile is not enough
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundAdditionOnSharedVolatileVariable", "getNum", 11);
    }

    // ^=
    @Test
    void reportsBugFor_compoundXOROperation_onSharedVariable() {
        // considered multithreaded because it has a field (not the problematic) with synchronized assignment
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundXOROperationOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundXOROperationOnSharedVariable", "getFlag", 16);
    }

    // num = num + 2
    @Test
    void reportsBugFor_simpleAddition_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/AdditionOnSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "AdditionOnSharedVariable", "getNum", 11);
    }

    // num = -num
    @Test
    void reportsBugFor_negate_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/NegateSharedVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "NegateSharedVariable", "getNum", 11);
    }

    // num -= num + 2
    @Test
    void reportsBugFor_compoundSubtraction_onSharedVariable_complex() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractComplexExpression.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundSubstractComplexExpression", "getNum", 11);
    }

    // num += num2 + 5
    @Test
    void reportsBugFor_compoundAddition_onSharedVariable_complex_withAnotherVar() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionComplexExpressionWithAnotherVar.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundAdditionComplexExpressionWithAnotherVar", "getNum", 12);
    }

    // num2 = num; num += 1
    @Test
    void reportsBugFor_compoundAddition_onSharedVariable_withAnotherVar() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionWithAnotherVar.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundAdditionWithAnotherVar", "getNum", 13);
    }

    // num += param
    @Test
    void reportsBugFor_compoundSubstractionOfArg_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractionOfArg.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundSubstractionOfArg", "getNum", 11);
    }

    // num += getOne()
    @Test
    void reportsBugFor_compoundSubstractionOfFunCall_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubstractionOfMethodReturnValue.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundSubstractionOfMethodReturnValue", "getNum", 11);
    }

    @Test
    void reportsBugFor_twoCompoundOperations_onSharedVariable() {
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivideMultiplyOnVariable.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "CompoundDivideMultiplyOnVariable", "multiply", 11);
    }
}
