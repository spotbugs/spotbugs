package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FieldIssuesTest extends AbstractIntegrationTest {

    @Test
    void testFieldsNotSetInConstructor() {
        performAnalysis("FieldsNotSetInConstructor.class");

        assertBugTypeCount("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", 1);
        assertBugAtField("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "FieldsNotSetInConstructor", "e");
    }

    @Test
    void testMaskMe() {
        performAnalysis("MaskMe.class", "MaskMe$DerivedMaskMe.class");

        assertBugTypeCount("MF_CLASS_MASKS_FIELD", 1);
        assertBugAtField("MF_CLASS_MASKS_FIELD", "MaskMe$DerivedMaskMe", "base_class_var");
    }

    @Test
    void testStaticInitializer() {
        performAnalysis("StaticInitializer.class");

        assertBugTypeCount("SI_INSTANCE_BEFORE_FINALS_ASSIGNED", 1);
    }

    @Test
    void testUnreadFields() {
        performAnalysis("UnreadFields.class");

        assertBugTypeCount("URF_UNREAD_FIELD", 1);
        assertBugAtField("URF_UNREAD_FIELD", "UnreadFields", "x");
    }

    @Test
    void testSelfAssignment() {
        performAnalysis("SelfAssignment.class");

        assertBugTypeCount("SA_LOCAL_SELF_ASSIGNMENT", 1);
        assertBugAtVar("SA_LOCAL_SELF_ASSIGNMENT", "SelfAssignment", "foo", "x", 4);
    }

    @Test
    void testOverwrittenParameter() {
        performAnalysis("OverwrittenParameter.class");

        assertBugTypeCount("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", 2);
        assertBugInMethod("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", "OverwrittenParameter", "f");
        assertBugInMethod("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", "OverwrittenParameter", "g");
    }
}
