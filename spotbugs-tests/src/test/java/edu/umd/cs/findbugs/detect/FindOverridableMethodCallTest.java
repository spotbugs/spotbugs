package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

import org.apache.bcel.Const;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class FindOverridableMethodCallTest extends AbstractIntegrationTest {

    @Test
    void testDirectCase() {
        testCase("DirectCase", 5, 18);
    }

    @Test
    void testDirectCaseObject() {
        testCase("DirectCaseObject", 5, 13);
    }

    @Test
    void testIndirectCase1() {
        testCase("IndirectCase1", 9, 22);
    }

    @Test
    void testIndirectCase2() {
        testCase("IndirectCase2", 5, 18);
    }

    @Test
    void testDoubleIndirectCase1() {
        testCase("DoubleIndirectCase1", 16, 29);
    }

    @Test
    void testDoubleIndirectCase2() {
        testCase("DoubleIndirectCase2", 16, 29);
    }

    @Test
    void testDoubleIndirectCase3() {
        testCase("DoubleIndirectCase3", 11, 24);
    }

    @Test
    void testDoubleIndirectCase4() {
        testCase("DoubleIndirectCase4", 10, 23);
    }

    @Test
    void testDoubleIndirectCase5() {
        testCase("DoubleIndirectCase5", 5, 18);
    }

    @Test
    void testDoubleIndirectCase6() {
        testCase("DoubleIndirectCase6", 5, 18);
    }

    @Test
    void testMethodReference() {
        testCase("MethodReference", 11, 20);
    }

    @Test
    void testMethodReferenceIndirect1() {
        testCase("MethodReferenceIndirect1", 15, 24);
    }

    @Test
    void testMethodReferenceIndirect2() {
        testCase("MethodReferenceIndirect2", 23, 32);
    }

    @Test
    void testMethodReferenceIndirect3() {
        testCase("MethodReferenceIndirect3", 23, 32);
    }

    @Test
    void testFinalClassDirect() {
        testPass("FinalClassDirect");
    }

    @Test
    void testFinalClassIndirect() {
        testPass("FinalClassIndirect");
    }

    @Test
    void testFinalClassDoubleIndirect() {
        testPass("FinalClassDoubleIndirect");
    }

    @Test
    void testFinalClassMethodReference() {
        testPass("FinalClassMethodReference");
    }

    @Test
    void testFinalClassInheritedDirect() {
        performAnalysis("overridableMethodCall/FinalClassInheritedDirect.class",
                "overridableMethodCall/InterfaceWithDefaultMethod.class");

        checkNoBug();
    }

    @Test
    void testFinalClassInheritedIndirect() {
        performAnalysis("overridableMethodCall/FinalClassInheritedIndirect.class",
                "overridableMethodCall/InterfaceWithDefaultMethod.class");

        checkNoBug();
    }

    @Test
    void testFinalClassInheritedDoubleIndirect() {
        performAnalysis("overridableMethodCall/FinalClassInheritedDoubleIndirect.class",
                "overridableMethodCall/InterfaceWithDefaultMethod.class");

        checkNoBug();
    }

    @Test
    void testFinalClassInheritedMethodReference() {
        performAnalysis("overridableMethodCall/FinalClassInheritedMethodReference.class",
                "overridableMethodCall/InterfaceWithDefaultMethod.class");

        checkNoBug();
    }

    @Test
    void testDirectReadObject() {
        testReadObject("DirectReadObject", 8);
    }

    @Test
    void testDirectReadObjectStreamMethods() {
        testPass("DirectReadObjectStreamMethods");
    }

    @Test
    void testDirectReadObjectStreamMethods2() {
        testReadObject("DirectReadObjectStreamMethods2", 11);
    }

    @Test
    void testIndirectReadObject1() {
        testReadObject("IndirectReadObject1", 11);
    }

    @Test
    void testIndirectReadObject2() {
        testReadObject("IndirectReadObject2", 7);
    }

    @Test
    void testIndirectStreamMethods1() {
        testPass("IndirectStreamMethods1");
    }

    @Test
    void testIndirectStreamMethods2() {
        testPass("IndirectStreamMethods2");
    }

    @Test
    void DoubleIndirectReadObjectCase1() {
        testReadObject("DoubleIndirectReadObjectCase1", 18);
    }

    @Test
    void DoubleIndirectReadObjectCase2() {
        testReadObject("DoubleIndirectReadObjectCase2", 18);
    }

    @Test
    void DoubleIndirectReadObjectCase3() {
        testReadObject("DoubleIndirectReadObjectCase3", 13);
    }

    @Test
    void DoubleIndirectReadObjectCase4() {
        testReadObject("DoubleIndirectReadObjectCase4", 12);
    }

    @Test
    void DoubleIndirectReadObjectCase5() {
        testReadObject("DoubleIndirectReadObjectCase5", 7);
    }

    @Test
    void DoubleIndirectReadObjectCase6() {
        testReadObject("DoubleIndirectReadObjectCase6", 7);
    }

    @Test
    void MethodReferenceReadObject() {
        testReadObject("MethodReferenceReadObject", 12);
    }

    @Test
    void MethodReferenceReadObjectIndirect1() {
        testReadObject("MethodReferenceReadObjectIndirect1", 16);
    }

    @Test
    void MethodReferenceReadObjectIndirect2() {
        testReadObject("MethodReferenceReadObjectIndirect2", 24);
    }

    @Test
    void MethodReferenceReadObjectIndirect3() {
        testReadObject("MethodReferenceReadObjectIndirect3", 24);
    }

    @Test
    void testFinalClassDirectReadObject() {
        testPass("FinalClassDirectReadObject");
    }

    @Test
    void testFinalClassIndirectReadObject() {
        testPass("FinalClassIndirectReadObject");
    }

    void testCase(String className, int constructorLine, int cloneLine) {
        performAnalysis("overridableMethodCall/" + className + ".class");

        checkOneBug();
        checkOverridableMethodCallInConstructor(className, constructorLine);
        checkOverridableMethodCallInClone(className, cloneLine);
    }

    void testReadObject(String className, int warningLine) {
        performAnalysis("overridableMethodCall/" + className + ".class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT")
                .inClass(className)
                .inMethod("readObject")
                .atLine(warningLine)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    void testPass(String className) {
        performAnalysis("overridableMethodCall/" + className + ".class");

        checkNoBug();
    }

    void checkOneBug() {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CLONE").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    void checkNoBug() {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CLONE").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
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
