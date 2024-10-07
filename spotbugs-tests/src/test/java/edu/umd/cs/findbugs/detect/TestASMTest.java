package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class TestASMTest extends AbstractIntegrationTest {

    @Test
    void testASM() {
        performAnalysis("TestASM.class");

        assertBugTypeCount("NM_METHOD_NAMING_CONVENTION", 1);
        assertBugTypeCount("NM_FIELD_NAMING_CONVENTION", 6);
        // It is reported both by TestASM and IDivResultCastToDouble
        assertBugTypeCount("ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL", 2);

        assertBugInMethod("NM_METHOD_NAMING_CONVENTION", "TestASM", "BadMethodName");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "TestASM", "badFieldNamePublicStaticFinal");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "TestASM", "BadFieldNamePublicStatic");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "TestASM", "BadFieldNamePublic");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "TestASM", "BadFieldNameProtectedStatic");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "TestASM", "BadFieldNameProtected");
        assertBugAtField("NM_FIELD_NAMING_CONVENTION", "TestASM", "BadFieldNamePrivate");
        assertBugInMethod("NM_METHOD_NAMING_CONVENTION", "TestASM", "BadMethodName");
        assertBugInMethod("ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL", "TestASM", "BadMethodName");
    }
}
