package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

class FindReturnRefTest extends AbstractIntegrationTest {
    @Test
    void testFindReturnRefTestNegativeChecks() {
        performAnalysis("FindReturnRefNegativeTest.class",
                "FindReturnRefNegativeTest$Inner.class");

        assertNoExposeBug();
    }

    @Test
    void testFindReturnRefTestChecks() {
        performAnalysis("FindReturnRefTest.class");

        assertBugTypeCount("EI_EXPOSE_BUF", 2);
        assertBugTypeCount("EI_EXPOSE_BUF2", 2);
        assertBugTypeCount("EI_EXPOSE_REP", 6);
        assertBugTypeCount("EI_EXPOSE_REP2", 6);
        assertBugTypeCount("EI_EXPOSE_STATIC_BUF2", 2);
        assertBugTypeCount("EI_EXPOSE_STATIC_REP2", 6);
        assertBugTypeCount("MS_EXPOSE_BUF", 2);
        assertBugTypeCount("MS_EXPOSE_REP", 6);

        assertBugInMethodAtField("EI_EXPOSE_BUF", "FindReturnRefTest", "getBufferWrap", "charArray");
        assertBugInMethodAtField("EI_EXPOSE_BUF", "FindReturnRefTest", "getBufferDuplicate", "charBuf");

        assertBugInMethodAtField("EI_EXPOSE_BUF2", "FindReturnRefTest", "setBufferDuplicate", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_BUF2", "FindReturnRefTest", "setBufferWrap", "charBuf");

        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest", "getBuffer", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest", "getDate", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest", "getDate2", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest", "getDateArray", "dateArray");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest", "getValues", "hm");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest", "getDateArray2", "dateArray");

        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest", "setBuffer", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest", "setDate", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest", "setDate2", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest", "setDateArray", "dateArray");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest", "setValues", "hm");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest", "setDateArray2", "dateArray");

        assertBugInMethodAtField("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest", "setStaticBufferDuplicate", "sCharBuf");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest", "setStaticBufferWrap", "sCharBuf");

        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "getStaticValues2", "shm");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticBuffer", "sCharBuf");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDate", "sDate");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDate2", "sDate");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDateArray", "sDateArray");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDateArray2", "sDateArray");

        assertBugInMethodAtField("MS_EXPOSE_BUF", "FindReturnRefTest", "getStaticBufferWrap", "sCharArray");
        assertBugInMethodAtField("MS_EXPOSE_BUF", "FindReturnRefTest", "getStaticBufferDuplicate", "sCharBuf");

        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticBuffer", "sCharBuf");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDate", "sDate");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDate2", "sDate");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDateArray", "sDateArray");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticValues", "shm");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDateArray2", "sDateArray");
    }

    @Test
    void testFindReturnRefTest2Checks() {
        performAnalysis("FindReturnRefTest2.class",
                "FindReturnRefTest2$Nested.class",
                "FindReturnRefTest2$Inner.class");

        assertBugTypeCount("EI_EXPOSE_BUF", 2);
        assertBugTypeCount("EI_EXPOSE_BUF2", 2);
        assertBugTypeCount("EI_EXPOSE_REP", 6);
        assertBugTypeCount("EI_EXPOSE_REP2", 6);
        assertBugTypeCount("EI_EXPOSE_STATIC_BUF2", 2);
        assertBugTypeCount("EI_EXPOSE_STATIC_REP2", 6);
        assertBugTypeCount("MS_EXPOSE_BUF", 2);
        assertBugTypeCount("MS_EXPOSE_REP", 6);

        assertBugInMethodAtField("EI_EXPOSE_BUF", "FindReturnRefTest2$Inner", "getBufferWrap", "charArray");
        assertBugInMethodAtField("EI_EXPOSE_BUF", "FindReturnRefTest2$Inner", "getBufferDuplicate", "charBuf");

        assertBugInMethodAtField("EI_EXPOSE_BUF2", "FindReturnRefTest2$Inner", "setBufferDuplicate", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_BUF2", "FindReturnRefTest2$Inner", "setBufferWrap", "charBuf");

        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getBuffer", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDate", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDate2", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDateArray", "dateArray");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getValues", "hm");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDateArray2", "dateArray");

        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setBuffer", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDate", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDate2", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDateArray", "dateArray");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setValues", "hm");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDateArray2", "dateArray");

        assertBugInMethodAtField("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest2$Nested", "setStaticBufferDuplicate", "sCharBuf");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest2$Nested", "setStaticBufferWrap", "sCharBuf");

        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticBuffer", "sCharBuf");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDate", "sDate");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDate2", "sDate");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDateArray", "sDateArray");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticValues", "shm");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDateArray2", "sDateArray");

        assertBugInMethodAtField("MS_EXPOSE_BUF", "FindReturnRefTest2$Nested", "getStaticBufferWrap", "sCharArray");
        assertBugInMethodAtField("MS_EXPOSE_BUF", "FindReturnRefTest2$Nested", "getStaticBufferDuplicate", "sCharBuf");

        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticBuffer", "sCharBuf");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDate", "sDate");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDate2", "sDate");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDateArray", "sDateArray");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticValues", "shm");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDateArray2", "sDateArray");
    }

    @Test
    void testFindReturnRefTest3Checks() {
        performAnalysis("FindReturnRefTest3.class",
                "FindReturnRefTest3$Nested.class");

        assertBugTypeCount("EI_EXPOSE_BUF", 2);
        assertBugTypeCount("EI_EXPOSE_BUF2", 2);
        assertBugTypeCount("EI_EXPOSE_REP", 6);
        assertBugTypeCount("EI_EXPOSE_REP2", 6);
        assertBugTypeCount("EI_EXPOSE_STATIC_BUF2", 2);
        assertBugTypeCount("EI_EXPOSE_STATIC_REP2", 6);
        assertBugTypeCount("MS_EXPOSE_BUF", 2);
        assertBugTypeCount("MS_EXPOSE_REP", 6);

        assertBugInMethodAtField("EI_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getBufferWrap", "charArray");
        assertBugInMethodAtField("EI_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getBufferDuplicate", "charBuf");

        assertBugInMethodAtField("EI_EXPOSE_BUF2", "FindReturnRefTest3$Nested", "setBufferDuplicate", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_BUF2", "FindReturnRefTest3$Nested", "setBufferWrap", "charBuf");

        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getBuffer", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDate", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDate2", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDateArray", "dateArray");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getValues", "hm");
        assertBugInMethodAtField("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDateArray2", "dateArray");

        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setBuffer", "charBuf");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDate", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDate2", "date");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDateArray", "dateArray");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setValues", "hm");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDateArray2", "dateArray");

        assertBugInMethodAtField("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest3$Nested", "setStaticBufferDuplicate", "sCharBuf");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest3$Nested", "setStaticBufferWrap", "sCharBuf");

        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticBuffer", "sCharBuf");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDate", "sDate");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDate2", "sDate");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDateArray", "sDateArray");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticValues", "shm");
        assertBugInMethodAtField("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDateArray2", "sDateArray");

        assertBugInMethodAtField("MS_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getStaticBufferWrap", "sCharArray");
        assertBugInMethodAtField("MS_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getStaticBufferDuplicate", "sCharBuf");

        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticBuffer", "sCharBuf");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDate", "sDate");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDate2", "sDate");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDateArray", "sDateArray");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticValues", "shm");
        assertBugInMethodAtField("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDateArray2", "sDateArray");
    }

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testUnmodifiableClass() {
        performAnalysis("../java17/exposemutable/UnmodifiableClass.class");

        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getListWithTernary2", "listWithTernary2");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getSetWithTernary2", "setWithTernary2");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getMapWithTernary2", "mapWithTernary2");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getListWithTernary3", "listWithTernary3");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getSetWithTernary3", "setWithTernary3");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getMapWithTernary3", "mapWithTernary3");

        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getListWithIf2", "listWithIf2");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getSetWithIf2", "setWithIf2");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getMapWithIf2", "mapWithIf2");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getListWithIf3", "listWithIf3");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getSetWithIf3", "setWithIf3");
        assertBugInMethodAtField("EI_EXPOSE_REP", "UnmodifiableClass", "getMapWithIf3", "mapWithIf3");

        assertBugTypeCount("EI_EXPOSE_REP", 12);
        assertBugTypeCount("EI_EXPOSE_BUF", 0);
        assertBugTypeCount("EI_EXPOSE_BUF2", 0);
        assertBugTypeCount("EI_EXPOSE_REP2", 0);
        assertBugTypeCount("EI_EXPOSE_STATIC_BUF2", 0);
        assertBugTypeCount("EI_EXPOSE_STATIC_REP2", 0);
        assertBugTypeCount("MS_EXPOSE_BUF", 0);
        assertBugTypeCount("MS_EXPOSE_REP", 0);
    }

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    @Disabled
    void testUnmodifiableRecord() {
        performAnalysis("../java17/exposemutable/UnmodifiableRecord.class");
        assertNoExposeBug();
    }

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testUnmodifiableRecordWithAllParamConstructor() {
        performAnalysis("../java17/exposemutable/UnmodifiableRecordWithAllParamConstructor.class");
        assertNoExposeBug();
    }

    private void assertNoExposeBug() {
        assertBugTypeCount("EI_EXPOSE_BUF", 0);
        assertBugTypeCount("EI_EXPOSE_BUF2", 0);
        assertBugTypeCount("EI_EXPOSE_REP", 0);
        assertBugTypeCount("EI_EXPOSE_REP2", 0);
        assertBugTypeCount("EI_EXPOSE_STATIC_BUF2", 0);
        assertBugTypeCount("EI_EXPOSE_STATIC_REP2", 0);
        assertBugTypeCount("MS_EXPOSE_BUF", 0);
        assertBugTypeCount("MS_EXPOSE_REP", 0);
    }
}
