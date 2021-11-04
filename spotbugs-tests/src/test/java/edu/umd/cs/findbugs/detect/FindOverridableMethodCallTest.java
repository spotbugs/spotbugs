package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

import org.apache.bcel.Const;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindOverridableMethodCallTest extends AbstractIntegrationTest {
    @Test
    public void testDirectCase() {
        testCase("DirectCase", 5, 18);
    }

    @Test
    public void testDirectCaseObject() {
        testCase("DirectCaseObject", 5, 13);
    }

    @Test
    public void testIndirectCase1() {
        testCase("IndirectCase1", 9, 22);
    }

    @Test
    public void testIndirectCase2() {
        testCase("IndirectCase2", 5, 18);
    }

    @Test
    public void testDoubleIndirectCase1() {
        testCase("DoubleIndirectCase1", 16, 29);
    }

    @Test
    public void testDoubleIndirectCase2() {
        testCase("DoubleIndirectCase2", 16, 29);
    }

    @Test
    public void testDoubleIndirectCase3() {
        testCase("DoubleIndirectCase3", 11, 24);
    }

    @Test
    public void testDoubleIndirectCase4() {
        testCase("DoubleIndirectCase4", 10, 23);
    }

    @Test
    public void testDoubleIndirectCase5() {
        testCase("DoubleIndirectCase5", 5, 18);
    }

    @Test
    public void testDoubleIndirectCase6() {
        testCase("DoubleIndirectCase6", 5, 18);
    }

    @Test
    public void testMethodReference() {
        testCase("MethodReference", 11, 20);
    }

    @Test
    public void testMethodReferenceIndirect1() {
        testCase("MethodReferenceIndirect1", 15, 24);
    }

    @Test
    public void testMethodReferenceIndirect2() {
        testCase("MethodReferenceIndirect2", 23, 32);
    }

    @Test
    public void testMethodReferenceIndirect3() {
        testCase("MethodReferenceIndirect3", 23, 32);
    }

    void testCase(String className, int constructorLine, int cloneLine) {
        performAnalysis("overridableMethodCall/" + className + ".class");

        checkOneBug();
        checkOverridableMethodCallInConstructor(className, constructorLine);
        checkOverridableMethodCallInClone(className, cloneLine);
    }

    void checkOneBug() {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CLONE").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    void checkOverridableMethodCallInConstructor(String className, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
                .inClass(className)
                .inMethod(Const.CONSTRUCTOR_NAME)
                .withConfidence(Confidence.LOW)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    void checkOverridableMethodCallInClone(String className, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CLONE")
                .inClass(className)
                .inMethod("clone")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
