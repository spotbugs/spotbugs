package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class ArrayEqualsTest extends AbstractIntegrationTest {
    @Test
    void testNegative() {
        performAnalysis("com/google/errorprone/bugpatterns/ArrayEqualsNegativeCases.class");
        assertNoBugType("EC_BAD_ARRAY_COMPARE");
    }

    @Test
    void testPositive() {
        performAnalysis("com/google/errorprone/bugpatterns/ArrayEqualsPositiveCases.class");
        assertBugTypeCount("EC_BAD_ARRAY_COMPARE", 5);

        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "intArray", 30);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "objectArray", 42);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "firstMethodCall", 54);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "secondMethodCall", 66);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "bothMethodCalls", 78);
    }

    @Test
    void testCompareArrays() {
        performAnalysis("CompareArrays.class");

        assertBugTypeCount("EC_BAD_ARRAY_COMPARE", 2);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "CompareArrays", "cmpArrays", 3);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "CompareArrays", "cmpArrays", 7);
    }

    @Test
    void testBadCompareTo() {
        performAnalysis("BadCompareTo.class", "GoodCompareTo.class");

        assertBugTypeCountBetween("EQ_COMPARETO_USE_OBJECT_EQUALS", 1, 2);
        // FP in GoodCompareTo
        assertBugInMethod("EQ_COMPARETO_USE_OBJECT_EQUALS", "BadCompareTo", "compareTo");
    }

    @Test
    void testCompareToButNoEquals() {
        performAnalysis("CompareToButNoEquals.class");

        assertBugTypeCount("EQ_COMPARETO_USE_OBJECT_EQUALS", 1);
        assertBugInMethod("EQ_COMPARETO_USE_OBJECT_EQUALS", "CompareToButNoEquals", "compareTo");
    }

    @Test
    void testCompareToFailure() {
        performAnalysis("CompareToFailure.class");

        assertBugTypeCount("EQ_COMPARETO_USE_OBJECT_EQUALS", 1);
        assertBugTypeCount("IL_INFINITE_RECURSIVE_LOOP", 1);
        assertBugInMethod("EQ_COMPARETO_USE_OBJECT_EQUALS", "CompareToFailure", "compareTo");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "CompareToFailure", "recurso");
    }

    @Test
    void testEq() {
        performAnalysis("Eq.class");

        assertBugTypeCount("EQ_ABSTRACT_SELF", 1);
    }

    @Test
    void testEqStringTestIsBetter() {
        performAnalysis("EqStringTestIsBetter.class");

        assertBugTypeCount("ES_COMPARING_PARAMETER_STRING_WITH_EQ", 2);
        assertBugInMethodAtLine("ES_COMPARING_PARAMETER_STRING_WITH_EQ", "EqStringTestIsBetter", "test", 3);
        assertBugInMethodAtLine("ES_COMPARING_PARAMETER_STRING_WITH_EQ", "EqStringTestIsBetter", "test", 6);
    }

    @Test
    void testEqualButNotEqual() {
        performAnalysis("EqualButNotEqual.class");

        assertBugTypeCount("EC_UNRELATED_TYPES", 1);
        assertBugTypeCount("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", 1);
        assertBugInMethodAtLine("EC_UNRELATED_TYPES", "EqualButNotEqual", "main", 8);
        assertBugInMethodAtLine("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", "EqualButNotEqual", "main", 7);
    }

    @Test
    void testEqualsComparison() {
        performAnalysis("EqualsComparison.class", "EqualsComparison$A.class", "EqualsComparison$B.class");

        assertBugTypeCount("EC_UNRELATED_TYPES", 2);
        assertBugTypeCount("EC_NULL_ARG", 1);
        assertBugInMethodAtLine("EC_UNRELATED_TYPES", "EqualsComparison", "badEqualsComparision", 12);
        assertBugInMethodAtLine("EC_UNRELATED_TYPES", "EqualsComparison", "unrelatedInterfaceComparison", 25);
        assertBugInMethodAtLine("EC_NULL_ARG", "EqualsComparison", "isEqualToNull", 20);
    }

    @Test
    void testForgotToOverrideEquals() {
        performAnalysis("ForgotToOverrideEquals.class", "ForgotToOverrideEquals$Oops.class");

        assertBugTypeCount("EQ_DOESNT_OVERRIDE_EQUALS", 1);
        assertBugInClass("EQ_DOESNT_OVERRIDE_EQUALS", "ForgotToOverrideEquals$Oops");
    }

    @Test
    void testHashcode() {
        performAnalysis("Hashcode.class");

        assertBugTypeCount("HE_HASHCODE_USE_OBJECT_EQUALS", 1);
        assertBugTypeCount("CO_ABSTRACT_SELF", 1);
        assertBugInMethod("HE_HASHCODE_USE_OBJECT_EQUALS", "Hashcode", "hashCode");
        assertBugInClass("CO_ABSTRACT_SELF", "Hashcode");
    }

    @Test
    void testITest() {
        performAnalysis("ITest.class", "ITest$A.class", "ITest$B.class");

        assertNoBugType("EC_UNRELATED_TYPES_USING_POINTER_EQUALITY");
        assertNoBugType("EC_UNRELATED_TYPES");
        assertNoBugType("EC_UNRELATED_INTERFACES");
        assertNoBugType("EC_UNRELATED_CLASS_AND_INTERFACE");
        assertNoBugType("EC_UNRELATED_CLASS_AND_INTERFACE");
    }

    @Test
    void testNoNaNCompare() {
        performAnalysis("NoNaNCompare.class");

        assertBugTypeCount("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", 2);
        assertBugInMethodAtLine("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", "NoNaNCompare", "checkIt", 3);
        assertBugInMethodAtLine("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", "NoNaNCompare", "checkIt", 7);
    }

    @Test
    void testStringEqualityTest() {
        performAnalysis("StringEqualityTest.class");

        assertBugTypeCount("ES_COMPARING_STRINGS_WITH_EQ", 1);
        assertBugInMethod("ES_COMPARING_STRINGS_WITH_EQ", "StringEqualityTest", "almostEmpty");
    }

    @Test
    void testStringEqualsTest() {
        performAnalysis("StringEqualsTest.class");

        assertBugTypeCount("ES_COMPARING_PARAMETER_STRING_WITH_EQ", 3);
        assertBugInMethod("ES_COMPARING_PARAMETER_STRING_WITH_EQ", "StringEqualsTest", "test");
        assertBugInMethod("ES_COMPARING_PARAMETER_STRING_WITH_EQ", "StringEqualsTest", "test2");
        assertBugInMethod("ES_COMPARING_PARAMETER_STRING_WITH_EQ", "StringEqualsTest", "test3");
    }
}
