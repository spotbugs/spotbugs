package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2793Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testRecordSerialVersionUid() {
        performAnalysis("../java17/ghIssues/Issue2793.class",
                "../java17/ghIssues/Issue2793$RecordWithoutSerialVersionUid.class",
                "../java17/ghIssues/Issue2793$ExternalizableRecord.class",
                "../java17/ghIssues/Issue2793$SerializableClass.class",
                "../java17/ghIssues/Issue2793$YangLibModule.class");

        assertBugTypeCount("SE_NO_SERIALVERSIONID", 2);

        assertNoBugType("SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION");

        assertNoBugType("SE_BAD_FIELD");
    }
}
