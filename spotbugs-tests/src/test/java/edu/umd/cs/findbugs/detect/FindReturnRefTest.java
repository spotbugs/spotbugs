package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindReturnRefTest extends AbstractIntegrationTest {
    @Test
    void testFindReturnRefTestChecks() {
        performAnalysis("FindReturnRefTest.class");

        assertNumOfBugs("EI_EXPOSE_BUF", 2);
        assertNumOfBugs("EI_EXPOSE_BUF2", 2);
        assertNumOfBugs("EI_EXPOSE_REP", 6);
        assertNumOfBugs("EI_EXPOSE_REP2", 6);
        assertNumOfBugs("EI_EXPOSE_STATIC_BUF2", 2);
        assertNumOfBugs("EI_EXPOSE_STATIC_REP2", 6);
        assertNumOfBugs("MS_EXPOSE_BUF", 2);
        assertNumOfBugs("MS_EXPOSE_REP", 6);

        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest", "getBuferWrap", "charArray");
        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest", "getBufferDuplicate", "charBuf");

        assertBug("EI_EXPOSE_BUF2", "FindReturnRefTest", "setBufferDuplicate", "charBuf");
        assertBug("EI_EXPOSE_BUF2", "FindReturnRefTest", "setBufferWrap", "charBuf");

        assertBug("EI_EXPOSE_REP", "FindReturnRefTest", "getBuffer", "charBuf");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest", "getDate", "date");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest", "getDate2", "date");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest", "getDateArray", "dateArray");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest", "getValues", "hm");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest", "getDateArray2", "dateArray");

        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest", "setBuffer", "charBuf");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest", "setDate", "date");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest", "setDate2", "date");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest", "setDateArray", "dateArray");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest", "setValues", "hm");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest", "setDateArray2", "dateArray");

        assertBug("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest", "setStaticBufferDuplicate", "sCharBuf");
        assertBug("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest", "setStaticBufferWrap", "sCharBuf");

        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "getStaticValues", "shm");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticBuffer", "sCharBuf");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDate", "sDate");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDate2", "sDate");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDateArray", "sDateArray");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDateArray2", "sDateArray");

        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest", "getStaticBuferWrap", "sCharArray");
        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest", "getStaticBufferDuplicate", "sCharBuf");

        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticBuffer", "sCharBuf");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDate", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDate2", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDateArray", "sDateArray");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticValues", "shm");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDateArray2", "sDateArray");

    }

    @Test
    void testFindReturnRefTest2Checks() {
        performAnalysis("FindReturnRefTest2.class",
                "FindReturnRefTest2$Nested.class",
                "FindReturnRefTest2$Inner.class");

        assertNumOfBugs("EI_EXPOSE_BUF", 2);
        assertNumOfBugs("EI_EXPOSE_BUF2", 2);
        assertNumOfBugs("EI_EXPOSE_REP", 6);
        assertNumOfBugs("EI_EXPOSE_REP2", 6);
        assertNumOfBugs("EI_EXPOSE_STATIC_BUF2", 2);
        assertNumOfBugs("EI_EXPOSE_STATIC_REP2", 6);
        assertNumOfBugs("MS_EXPOSE_BUF", 2);
        assertNumOfBugs("MS_EXPOSE_REP", 6);

        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest2$Inner", "getBuferWrap", "charArray");
        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest2$Inner", "getBufferDuplicate", "charBuf");

        assertBug("EI_EXPOSE_BUF2", "FindReturnRefTest2$Inner", "setBufferDuplicate", "access$802");
        assertBug("EI_EXPOSE_BUF2", "FindReturnRefTest2$Inner", "setBufferWrap", "access$802");

        assertBug("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getBuffer", "charBuf");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDate", "date");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDate2", "date");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDateArray", "dateArray");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getValues", "hm");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest2$Inner", "getDateArray2", "dateArray");

        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setBuffer", "access$802");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDate", "access$502");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDate2", "access$502");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDateArray", "access$602");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setValues", "access$702");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest2$Inner", "setDateArray2", "access$602");

        assertBug("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest2$Nested", "setStaticBufferDuplicate", "access$302");
        assertBug("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest2$Nested", "setStaticBufferWrap", "access$302");

        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticBuffer", "access$302");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDate", "access$002");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDate2", "access$002");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDateArray", "access$102");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticValues", "access$202");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest2$Nested", "setStaticDateArray2", "access$102");

        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest2$Nested", "getStaticBuferWrap", "sCharArray");
        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest2$Nested", "getStaticBufferDuplicate", "sCharBuf");

        assertBug("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticBuffer", "sCharBuf");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDate", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDate2", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDateArray", "sDateArray");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticValues", "shm");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest2$Nested", "getStaticDateArray2", "sDateArray");

    }

    @Test
    void testFindReturnRefTest3Checks() {
        performAnalysis("FindReturnRefTest3.class",
                "FindReturnRefTest3$Nested.class");

        assertNumOfBugs("EI_EXPOSE_BUF", 2);
        assertNumOfBugs("EI_EXPOSE_BUF2", 2);
        assertNumOfBugs("EI_EXPOSE_REP", 6);
        assertNumOfBugs("EI_EXPOSE_REP2", 6);
        assertNumOfBugs("EI_EXPOSE_STATIC_BUF2", 2);
        assertNumOfBugs("EI_EXPOSE_STATIC_REP2", 6);
        assertNumOfBugs("MS_EXPOSE_BUF", 2);
        assertNumOfBugs("MS_EXPOSE_REP", 6);

        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getBuferWrap", "charArray");
        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getBufferDuplicate", "charBuf");

        assertBug("EI_EXPOSE_BUF2", "FindReturnRefTest3$Nested", "setBufferDuplicate", "charBuf");
        assertBug("EI_EXPOSE_BUF2", "FindReturnRefTest3$Nested", "setBufferWrap", "charBuf");

        assertBug("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getBuffer", "charBuf");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDate", "date");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDate2", "date");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDateArray", "dateArray");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getValues", "hm");
        assertBug("EI_EXPOSE_REP", "FindReturnRefTest3$Nested", "getDateArray2", "dateArray");

        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setBuffer", "charBuf");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDate", "date");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDate2", "date");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDateArray", "dateArray");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setValues", "hm");
        assertBug("EI_EXPOSE_REP2", "FindReturnRefTest3$Nested", "setDateArray2", "dateArray");

        assertBug("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest3$Nested", "setStaticBufferDuplicate", "sCharBuf");
        assertBug("EI_EXPOSE_STATIC_BUF2", "FindReturnRefTest3$Nested", "setStaticBufferWrap", "sCharBuf");

        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticBuffer", "sCharBuf");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDate", "sDate");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDate2", "sDate");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDateArray", "sDateArray");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticValues", "shm");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest3$Nested", "setStaticDateArray2", "sDateArray");

        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getStaticBuferWrap", "sCharArray");
        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest3$Nested", "getStaticBufferDuplicate", "sCharBuf");

        assertBug("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticBuffer", "sCharBuf");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDate", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDate2", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDateArray", "sDateArray");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticValues", "shm");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest3$Nested", "getStaticDateArray2", "sDateArray");
    }

    @Test
    void testFindReturnRefTestNegativeChecks() {
        performAnalysis("FindReturnRefNegativeTest.class",
                "FindReturnRefNegativeTest$Inner.class");

        assertNumOfBugs("EI_EXPOSE_BUF", 0);
        assertNumOfBugs("EI_EXPOSE_BUF2", 0);
        assertNumOfBugs("EI_EXPOSE_REP", 0);
        assertNumOfBugs("EI_EXPOSE_REP2", 0);
        assertNumOfBugs("EI_EXPOSE_STATIC_BUF2", 0);
        assertNumOfBugs("EI_EXPOSE_STATIC_REP2", 0);
        assertNumOfBugs("MS_EXPOSE_BUF", 0);
        assertNumOfBugs("MS_EXPOSE_REP", 0);
    }

    private void assertNumOfBugs(String bugtype, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug(String bugtype, String className, String method, String field) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass(className)
                .inMethod(method)
                .atField(field)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
