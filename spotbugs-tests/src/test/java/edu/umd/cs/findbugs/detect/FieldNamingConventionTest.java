package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FieldNamingConventionTest extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("badNaming/FieldNamingConvention.class");
        assertBugTypeCount("NM_FIELD_NAMING_CONVENTION", 1);
    }

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
    void testFalseException() {
        performAnalysis("FalseException.class");

        assertBugTypeCount("NM_CLASS_NOT_EXCEPTION", 1);
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
