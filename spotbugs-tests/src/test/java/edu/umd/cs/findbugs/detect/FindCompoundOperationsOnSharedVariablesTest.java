package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

import static org.hamcrest.MatcherAssert.assertThat;

public class FindCompoundOperationsOnSharedVariablesTest extends AbstractIntegrationTest {

    @Test
    public void happyPath_compoundOperation_onSharedAtomicVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundOperationOnSharedAtomicVariable.class");

        assertCOSVNumOfBugs(0);
    }

    @Test
    public void happyPath_compoundOperationInsideSynchronizedBlock_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedBlockCompoundOperationOnSharedVariable.class");

        assertCOSVNumOfBugs(0);
    }

    @Test
    public void happyPath_compoundOperationInsideSynchronizedMethod_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/SynchronizedMethodCompoundOperationOnSharedVariable.class");

        assertCOSVNumOfBugs(0);
    }

    // --num
    @Test
    public void reportsBugFor_compoundPreDecrementation_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreDecrementationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPreDecrementationOnSharedVariable", "getNum", 11);
    }

    // num--
    @Test
    public void reportsBugFor_compoundPostDecrementation_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostDecrementationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPostDecrementationOnSharedVariable", "getNum", 11);
    }

    // ++num
    @Test
    public void reportsBugFor_compoundPreIncrementation_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPreIncrementationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPreIncrementationOnSharedVariable", "getNum", 11);
    }

    // num++
    @Test
    public void reportsBugFor_compoundPostIncrementation_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundPostIncrementationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundPostIncrementationOnSharedVariable", "getNum", 11);
    }

    // &=
    @Test
    public void reportsBugFor_compoundIAND_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIANDOperationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundIANDOperationOnSharedVariable", "getNum", 11);
    }

    // |=
    @Test
    public void reportsBugFor_compoundIOR_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundIOROperationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundIOROperationOnSharedVariable", "getNum", 11);
    }

    // >>>=
    @Test
    public void reportsBugFor_compoundLogicalRightShifting_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLogicalRightShiftingOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundLogicalRightShiftingOnSharedVariable", "getNum", 11);
    }

    // >>=
    @Test
    public void reportsBugFor_compoundRightShifting_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundRightShiftingOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundRightShiftingOnSharedVariable", "getNum", 11);
    }

    // <<=
    @Test
    public void reportsBugFor_compoundLeftShifting_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundLeftShiftingOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundLeftShiftingOnSharedVariable", "getNum", 11);
    }

    // %=
    @Test
    public void reportsBugFor_compoundModulo_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundModuloOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundModuloOnSharedVariable", "getNum", 12);
    }

    // *=
    @Test
    public void reportsBugFor_compoundMultiplication_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundMultiplicationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundMultiplicationOnSharedVariable", "getNum", 11);
    }

    // /=
    @Test
    public void reportsBugFor_compoundDivision_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundDivisionOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundDivisionOnSharedVariable", "getNum", 11);
    }

    // -=
    @Test
    public void reportsBugFor_compoundSubtraction_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundSubtractionOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundSubtractionOnSharedVariable", "getNum", 11);
    }


    // +=
    @Test
    public void reportsBugFor_compoundAddition_onSharedVolatileVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundAdditionOnSharedVolatileVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundAdditionOnSharedVolatileVariable", "getNum", 11);
    }

    // ^=
    @Test
    public void reportsBugFor_compoundXOROperation_onSharedVariable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/compoundOperationOnSharedVariables/CompoundXOROperationOnSharedVariable.class");

        assertCOSVNumOfBugs(1);
        assertCOSVBug("CompoundXOROperationOnSharedVariable", "getFlag", 11);
    }

    private void assertCOSVNumOfBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES")
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertCOSVBug(String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("COSV_COMPOUND_OPERATIONS_ON_SHARED_VARIABLES")
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
