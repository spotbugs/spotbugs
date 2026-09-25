package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class MissingTest extends AbstractIntegrationTest {
    @Test
    void testAccidentalNonConstructor() {
        performAnalysis("AccidentalNonConstructorInInnerClass.class",
                "AccidentalNonConstructorInInnerClass$Report.class",
                "AccidentalNonConstructorInInnerClass$Report$DeeplyNested.class",
                "AccidentalNonConstructorInInnerClass$Report2.class",
                "AccidentalNonConstructorInInnerClass$DoNotReport.class",
                "AccidentalNonConstructorInInnerClass$DoNotReport2.class");

        assertBugTypeCount("NM_METHOD_CONSTRUCTOR_CONFUSION", 3);
        assertBugInMethod("NM_METHOD_CONSTRUCTOR_CONFUSION", "AccidentalNonConstructorInInnerClass$Report", "Report");
        assertBugInMethod("NM_METHOD_CONSTRUCTOR_CONFUSION", "AccidentalNonConstructorInInnerClass$Report$DeeplyNested", "DeeplyNested");
        assertBugInMethod("NM_METHOD_CONSTRUCTOR_CONFUSION", "AccidentalNonConstructorInInnerClass$Report2", "Report2");
    }

    @Test
    void testBadCompareTo() {
        performAnalysis("BadCompareTo.class", "GoodCompareTo.class");

        assertBugTypeCountBetween("EQ_COMPARETO_USE_OBJECT_EQUALS", 1, 2);
        // FP in GoodCompareTo
        assertBugInMethod("EQ_COMPARETO_USE_OBJECT_EQUALS", "BadCompareTo", "compareTo");
    }

    @Test
    void testBadRemainderCheck() {
        performAnalysis("BadRemainderCheck.class");

        assertBugTypeCount("IM_MULTIPLYING_RESULT_OF_IREM", 1);
        assertBugInMethod("IM_MULTIPLYING_RESULT_OF_IREM", "BadRemainderCheck", "isOnHourBoundary");
    }

    @Test
    void testBadThingsToDoWithSignedBytes() {
        performAnalysis("BadThingsToDoWithSignedBytes.class");

        assertBugTypeCount("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", 16);
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareGT127");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareLE127");
        // PR #4201 made compareGE127 and compareLT127 acceptable usages based on Issue #4192

        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareGT128");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareGE128");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareNE128");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareEQ128");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareLE128");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareLT128");

        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareGT200");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareGE200");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareNE200");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareEQ200");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareLE200");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "compareLT200");

        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "isHundred");
        assertBugInMethod("INT_BAD_COMPARISON_WITH_SIGNED_BYTE", "BadThingsToDoWithSignedBytes", "isHundred2");
    }

    @Test
    void testCallSystemExit() {
        performAnalysis("CallSystemExit.class");

        assertBugTypeCount("DM_EXIT", 1);
        assertBugInMethod("DM_EXIT", "CallSystemExit", "equals");
    }

    @Test
    void testCastOfArray() {
        performAnalysis("CastOfArray.class");

        assertNoBugType("BC_BAD_CAST_TO_ABSTRACT_COLLECTION");
        assertNoBugType("BC_IMPOSSIBLE_CAST_PRIMITIVE_ARRAY");
        assertNoBugType("BC_IMPOSSIBLE_CAST");
        assertNoBugType("BC_IMPOSSIBLE_DOWNCAST");
        assertNoBugType("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY");
        assertNoBugType("BC_UNCONFIRMED_CAST");
        assertNoBugType("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE");
        assertNoBugType("BC_BAD_CAST_TO_CONCRETE_COLLECTION");
        assertNoBugType("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY");
    }

    @Test
    void testBC_Unconfirmed_Cast() {
        performAnalysis("BC_Unconfirmed_Cast.class", "BC_Unconfirmed_Cast$CastToMe.class",
                "Parent.class", "Child1.class", "Child2.class");

        assertBugTypeCount("BC_IMPOSSIBLE_CAST", 2);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "BC_Unconfirmed_Cast", "main", 16);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "BC_Unconfirmed_Cast", "main", 18);
    }

    @Test
    void testCircularClassInitialization() {
        performAnalysis("CircularClassInitialization.class", "CircularClassInitialization$InnerClassSingleton.class");

        assertBugTypeCount("IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", 1);
        assertBugInClass("IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", "CircularClassInitialization");
    }

    @Test
    void testCircularDepsTest() {
        performAnalysis("CircularDepsTest.class", "Circ1.class", "Circ2.class", "NoCirc.class");

        assertBugTypeCount("CD_CIRCULAR_DEPENDENCY", 2);
        assertBugInClassCount("CD_CIRCULAR_DEPENDENCY", "CircularDepsTest", 2); // no more details provided for the bug
    }

    @Test
    void testCloneIdiom() {
        performAnalysis("CloneIdiom1.class", "CloneIdiom2.class", "CloneIdiom3.class");

        assertBugTypeCount("CN_IDIOM", 1);
        assertBugTypeCount("CN_IDIOM_NO_SUPER_CALL", 1);

        assertBugInClass("CN_IDIOM", "CloneIdiom1");
        assertBugInClass("CN_IDIOM_NO_SUPER_CALL", "CloneIdiom3");
    }

    @Test
    void testCloneStringArray() {
        performAnalysis("CloneStringArray.class");

        assertBugTypeCount("DMI_INVOKING_TOSTRING_ON_ARRAY", 1);
        assertBugInMethodAtLine("DMI_INVOKING_TOSTRING_ON_ARRAY", "CloneStringArray", "main", 4);
    }

    @Test
    void testCompareArrays() {
        performAnalysis("CompareArrays.class");

        assertBugTypeCount("EC_BAD_ARRAY_COMPARE", 2);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "CompareArrays", "cmpArrays", 3);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "CompareArrays", "cmpArrays", 7);
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
    void testConfusify() {
        performAnalysis("Confusify.class", "Confusify$YarrBlarr.class", "Confusify$Helper.class");

        assertBugTypeCount("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", 1);
        assertBugInMethod("IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", "Confusify$Helper", "yarrOrBlarr");
    }

    @Test
    void testConfusingParenting() {
        performAnalysis("ConfusingParenting.class");

        assertBugTypeCount("CI_CONFUSED_INHERITANCE", 2);
        assertBugAtField("CI_CONFUSED_INHERITANCE", "ConfusingParenting", "a");
        assertBugAtField("CI_CONFUSED_INHERITANCE", "ConfusingParenting", "b");
    }

    @Test
    void testDETest() {
        performAnalysis("DETest.class");

        assertBugTypeCount("DE_MIGHT_IGNORE", 1);
        assertBugInMethod("DE_MIGHT_IGNORE", "DETest", "main");
    }

    @Test
    void testDontCatchIllegalMonitor() {
        performAnalysis("DontCatchIllegalMonitor.class");

        assertBugTypeCount("IMSE_DONT_CATCH_IMSE", 1);
        assertBugInMethod("IMSE_DONT_CATCH_IMSE", "DontCatchIllegalMonitor", "foo");
    }

    @Test
    void testDoublecheck() {
        performAnalysis("Doublecheck.class");

        assertBugTypeCountBetween("DC_DOUBLECHECK", 2, 4);
        // FNs: stringDoubleCheck, longDoubleCheck
        assertBugInMethod("DC_DOUBLECHECK", "Doublecheck", "standardDoubleCheck");
        assertBugInMethod("DC_DOUBLECHECK", "Doublecheck", "getData");
    }

    @Test
    void testDuplicateBranches() {
        performAnalysis("DuplicateBranches.class");

        assertBugTypeCount("DB_DUPLICATE_BRANCHES", 1);
        assertBugTypeCountBetween("DB_DUPLICATE_SWITCH_CLAUSES", 1, 2);
        // FNs: doit3
        assertBugInMethod("DB_DUPLICATE_BRANCHES", "DuplicateBranches", "doit");
        assertBugInMethod("DB_DUPLICATE_SWITCH_CLAUSES", "DuplicateBranches", "doit2");
    }

    @Test
    void testEI2() {
        performAnalysis("EI2.class");

        assertBugTypeCount("EI_EXPOSE_REP2", 1);
        assertBugInMethod("EI_EXPOSE_REP2", "EI2", "setStuff");
    }

    @Test
    void testEmptyIfStatement() {
        performAnalysis("EmptyIfStatement.class");

        assertBugTypeCount("UCF_USELESS_CONTROL_FLOW", 1);
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "EmptyIfStatement", "main");
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
    void testFalseException() {
        performAnalysis("FalseException.class");

        assertBugTypeCount("NM_CLASS_NOT_EXCEPTION", 1);
    }

    @Test
    void testFieldsNotSetInConstructor() {
        performAnalysis("FieldsNotSetInConstructor.class");

        assertBugTypeCount("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", 1);
        assertBugAtField("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "FieldsNotSetInConstructor", "e");
    }

    @Test
    void testFinalize() {
        performAnalysis("Finalize.class");

        assertBugTypeCount("FI_EMPTY", 1);
        assertBugInMethod("FI_EMPTY", "Finalize", "finalize");
    }

    @Test
    void testFloatMath() {
        performAnalysis("FloatMath.class");

        assertBugTypeCount("FL_FLOATS_AS_LOOP_COUNTERS", 1);
        assertBugInMethodAtLine("FL_FLOATS_AS_LOOP_COUNTERS", "FloatMath", "main", 5);
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
    void testIDiv() {
        performAnalysis("IDiv.class");

        assertBugTypeCount("ICAST_IDIV_CAST_TO_DOUBLE", 1);
        assertBugInMethodAtLine("ICAST_IDIV_CAST_TO_DOUBLE", "IDiv", "main", 6);
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
    void testIgnoredTrim() {
        performAnalysis("IgnoredTrim.class");

        assertBugTypeCount("RV_RETURN_VALUE_IGNORED", 1);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED", "IgnoredTrim", "f", 4);
    }

    @Test
    void testInfiniteRecursiveLoop() {
        performAnalysis("InfiniteRecursiveLoop.class");

        assertBugTypeCount("IL_INFINITE_RECURSIVE_LOOP", 5);
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "<init>");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "more");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "muchMore");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "equals");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "hashCode");

    }

    @Test
    void testInfiniteRecursiveLoopFalsePositive() {
        performAnalysis("InfiniteRecursiveLoopFalsePositive.class", "InfiniteRecursiveLoopFalsePositive$Inner.class");

        assertNoBugType("IL_INFINITE_RECURSIVE_LOOP");
    }

    @Test
    void testInvokeGC() {
        performAnalysis("InvokeGC.class");

        assertBugTypeCountBetween("DM_GC", 3, 4);
        // FN in tricky
        assertBugInMethod("DM_GC", "InvokeGC", "bad");
        assertBugInMethod("DM_GC", "InvokeGC", "finalize");
        assertBugInMethod("DM_GC", "InvokeGC", "tricky2");
    }

    @Test
    void testMaskMe() {
        performAnalysis("MaskMe.class", "MaskMe$DerivedMaskMe.class");

        assertBugTypeCount("MF_CLASS_MASKS_FIELD", 1);
        assertBugAtField("MF_CLASS_MASKS_FIELD", "MaskMe$DerivedMaskMe", "base_class_var");
    }

    @Test
    void testMismatchedWaitFalsePositive() {
        performAnalysis("MismatchedWaitFalsePositive.class");

        assertNoBugType("MWN_MISMATCHED_WAIT");
    }

    @Test
    void testModuloFailure() {
        performAnalysis("ModuloFailure.class");

        assertBugTypeCount("IM_MULTIPLYING_RESULT_OF_IREM", 1);
        assertBugInMethodAtLine("IM_MULTIPLYING_RESULT_OF_IREM", "ModuloFailure", "main", 9);
        // FN at line 6 (if x is negative)
    }

    @Test
    void testMutableMan() {
        performAnalysis("MutableMan.class");

        assertBugTypeCount("EI_EXPOSE_REP", 1);
        assertBugAtField("EI_EXPOSE_REP", "MutableMan", "x");
    }

    @Test
    void testNaming() {
        performAnalysis("Naming.class",
                "Naming$FinalException.class",
                "Naming$NamingException.class",
                "Naming$NamingBaseException.class",
                "Naming$TrickyName.class",
                "Naming$NamingBaseChildException.class");

        assertBugTypeCount("NM_CLASS_NOT_EXCEPTION", 1);
        assertBugInClass("NM_CLASS_NOT_EXCEPTION", "Naming$NamingException");
    }

    @Test
    void testNoNaNCompare() {
        performAnalysis("NoNaNCompare.class");

        assertBugTypeCount("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", 2);
        assertBugInMethodAtLine("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", "NoNaNCompare", "checkIt", 3);
        assertBugInMethodAtLine("FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", "NoNaNCompare", "checkIt", 7);
    }

    @Test
    void testNonShortCircuit() {
        performAnalysis("NonShortCircuit.class");

        assertBugTypeCount("NS_NON_SHORT_CIRCUIT", 3);
        assertBugTypeCount("NS_DANGEROUS_NON_SHORT_CIRCUIT", 1);
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "NonShortCircuit", "f");
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "NonShortCircuit", "nonEmpty");
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "NonShortCircuit", "ordered");
        assertBugInMethod("NS_DANGEROUS_NON_SHORT_CIRCUIT", "NonShortCircuit", "arrayDanger");
    }

    @Test
    void testNoopThread() {
        performAnalysis("NoopThread.class");

        assertBugTypeCount("DM_USELESS_THREAD", 3);
        assertBugInMethod("DM_USELESS_THREAD", "NoopThread", "test1");
        assertBugInMethod("DM_USELESS_THREAD", "NoopThread", "test2");
        assertBugInMethod("DM_USELESS_THREAD", "NoopThread", "test3");
    }

    @Test
    void testNotThreadSafe() {
        performAnalysis("NotThreadSafe.class", "ThreadSafe.class");

        assertBugTypeCount("IS2_INCONSISTENT_SYNC", 2);
        assertBugAtField("IS2_INCONSISTENT_SYNC", "ThreadSafe", "x");
        assertBugAtField("IS2_INCONSISTENT_SYNC", "ThreadSafe", "y");
    }

    @Test
    void testOverwrittenParameter() {
        performAnalysis("OverwrittenParameter.class");

        assertBugTypeCount("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", 2);
        assertBugInMethod("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", "OverwrittenParameter", "f");
        assertBugInMethod("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", "OverwrittenParameter", "g");
    }

    @Test
    void testPreferZeroLengthArrays() {
        performAnalysis("PreferZeroLengthArrays.class");

        assertBugTypeCount("PZLA_PREFER_ZERO_LENGTH_ARRAYS", 1);
        assertBugInMethod("PZLA_PREFER_ZERO_LENGTH_ARRAYS", "PreferZeroLengthArrays", "foo");
    }

    @Test
    void testProtectedMemberOfFinalClass() {
        performAnalysis("ProtectedMemberOfFinalClass.class");

        assertBugTypeCount("CI_CONFUSED_INHERITANCE", 1);
        assertBugAtField("CI_CONFUSED_INHERITANCE", "ProtectedMemberOfFinalClass", "foo");
    }

    @Test
    void testQuestionableBooleanAssignmentInAssertStatement() {
        performAnalysis("QuestionableBooleanAssignmentInAssertStatement.class");

        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT", 1);
        assertBugInMethodAtLine("ASE_ASSERTION_WITH_SIDE_EFFECT", "QuestionableBooleanAssignmentInAssertStatement", "main", 5);
    }

    @Test
    void testRV() {
        performAnalysis("RV.class");

        assertBugTypeCount("RV_EXCEPTION_NOT_THROWN", 1);
        assertBugTypeCount("RV_RETURN_VALUE_IGNORED", 2);
        assertBugTypeCount("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", 1);
        assertBugInMethodAtLine("RV_EXCEPTION_NOT_THROWN", "RV", "f", 11);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED", "RV", "f", 10);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED", "RV", "main", 5);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "RV", "g", 15);
    }

    @Test
    void testRandomStuff() {
        performAnalysis("RandomStuff.class");

        assertBugTypeCount("DM_NEXTINT_VIA_NEXTDOUBLE", 1);
        assertBugTypeCount("RV_01_TO_INT", 1);
        assertBugInMethodAtLine("DM_NEXTINT_VIA_NEXTDOUBLE", "RandomStuff", "screwAround", 17);
        assertBugInMethodAtLine("RV_01_TO_INT", "RandomStuff", "screwAround", 38);
    }

    @Test
    void testSBConcatTest() {
        performAnalysis("SBConcatTest.class");

        assertBugTypeCount("SBSC_USE_STRINGBUFFER_CONCATENATION", 2);
        assertBugInMethod("SBSC_USE_STRINGBUFFER_CONCATENATION", "SBConcatTest", "doConcat1");
        assertBugInMethod("SBSC_USE_STRINGBUFFER_CONCATENATION", "SBConcatTest", "doConcat2");
    }

    @Test
    void testSelfAssignment() {
        performAnalysis("SelfAssignment.class");

        assertBugTypeCount("SA_LOCAL_SELF_ASSIGNMENT", 1);
        assertBugAtVar("SA_LOCAL_SELF_ASSIGNMENT", "SelfAssignment", "foo", "x", 4);
    }

    @Test
    void testSleepWithLock() {
        performAnalysis("SleepWithLock.class");

        assertBugTypeCount("SWL_SLEEP_WITH_LOCK_HELD", 1);
        assertBugInMethod("SWL_SLEEP_WITH_LOCK_HELD", "SleepWithLock", "sleepWithLock");
    }

    @Test
    void testSpinWait() {
        performAnalysis("SpinWait.class");

        assertBugTypeCount("SP_SPIN_ON_FIELD", 6);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForTrue", 7);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForVolatileTrue", 12);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForNonNull", 21);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForNonNullIndirect", 26);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForNonNullIndirect", 28);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForStatic", 35);
    }

    @Test
    void testStaticInitializer() {
        performAnalysis("StaticInitializer.class");

        assertBugTypeCount("SI_INSTANCE_BEFORE_FINALS_ASSIGNED", 1);
    }

    @Test
    void testStringCompare() {
        performAnalysis("StringCompare.class");

        assertBugTypeCount("NS_DANGEROUS_NON_SHORT_CIRCUIT", 1);
        assertBugTypeCount("NS_NON_SHORT_CIRCUIT", 2);
        assertBugInMethodAtLine("NS_DANGEROUS_NON_SHORT_CIRCUIT", "StringCompare", "compare2", 13);
        assertBugInMethodAtLine("NS_NON_SHORT_CIRCUIT", "StringCompare", "compare", 5);
        assertBugInMethodAtLine("NS_NON_SHORT_CIRCUIT", "StringCompare", "compare", 9);
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

    @Test
    void testTestFalsePositiveMWN() {
        performAnalysis("TestFalsePositiveMWN.class");

        assertNoBugType("MWN_MISMATCHED_WAIT");
        assertNoBugType("MWN_MISMATCHED_NOTIFY");
    }

    @Test
    void testTestFloatEquality() {
        performAnalysis("TestFloatEquality.class");

        assertBugTypeCountBetween("FE_FLOATING_POINT_EQUALITY", 3, 7);
        // 4 FNs in main
        assertBugInMethodAtLine("FE_FLOATING_POINT_EQUALITY", "TestFloatEquality", "validCoin100", 5);
        assertBugInMethodAtLine("FE_FLOATING_POINT_EQUALITY", "TestFloatEquality", "sum", 73);
        assertBugInMethodAtLine("FE_FLOATING_POINT_EQUALITY", "TestFloatEquality", "isMyDouble", 81);
    }

    @Test
    void testTwoLockWait() {
        performAnalysis("TwoLockWait.class");

        assertBugTypeCount("TLW_TWO_LOCK_WAIT", 2);
        assertBugInMethod("TLW_TWO_LOCK_WAIT", "TwoLockWait", "waitForIt");
        assertBugInMethod("TLW_TWO_LOCK_WAIT", "TwoLockWait", "myMethod");
    }

    @Test
    void testTwoLocksWhileWaitingFalsePositive() {
        performAnalysis("TwoLocksWhileWaitingFalsePositive.class");
        assertNoBugType("TLW_TWO_LOCK_WAIT");
    }

    @Test
    void testUnreadFields() {
        performAnalysis("UnreadFields.class");

        assertBugTypeCount("URF_UNREAD_FIELD", 1);
        assertBugAtField("URF_UNREAD_FIELD", "UnreadFields", "x");
    }

    @Test
    void testUselessControlFlow() {
        performAnalysis("UselessControlFlow.class");

        assertBugTypeCount("UCF_USELESS_CONTROL_FLOW", 6);
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "harmless1");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report0");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report1");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report2");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report3");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report4");
    }

    @Test
    void testUselessFinalize() {
        performAnalysis("UselessFinalize.class");

        assertBugTypeCount("FI_USELESS", 1);
        assertBugInMethod("FI_USELESS", "UselessFinalize", "finalize");
    }

    @Test
    void testUselessSCMethods() {
        performAnalysis("UselessSCMethods.class", "Super.class");

        assertBugTypeCount("USM_USELESS_SUBCLASS_METHOD", 4);
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test1");
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test2");
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test3");
        assertBugInMethod("USM_USELESS_SUBCLASS_METHOD", "UselessSCMethods", "test5");
    }

    @Test
    void testhashCODEnoEQUALS() {
        performAnalysis("hashCODEnoEQUALS.class");

        assertBugTypeCount("HE_HASHCODE_USE_OBJECT_EQUALS", 1);
        assertBugTypeCount("NM_CLASS_NAMING_CONVENTION", 1);
        assertBugTypeCountBetween("NM_FIELD_NAMING_CONVENTION", 1, 2);
        // FN MOJOJOJO field naming

        assertBugInMethod("HE_HASHCODE_USE_OBJECT_EQUALS", "hashCODEnoEQUALS", "hashCode");
        assertBugInClass("NM_CLASS_NAMING_CONVENTION", "hashCODEnoEQUALS");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "hashCODEnoEQUALS", "ReuVeN");
    }
}
