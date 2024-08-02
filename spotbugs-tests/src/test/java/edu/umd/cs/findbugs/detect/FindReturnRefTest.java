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

        assertBug("EI_EXPOSE_BUF", "FindReturnRefTest", "getBufferWrap", "charArray");
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

        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "getStaticValues2", "shm");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticBuffer", "sCharBuf");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDate", "sDate");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDate2", "sDate");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDateArray", "sDateArray");
        assertBug("EI_EXPOSE_STATIC_REP2", "FindReturnRefTest", "setStaticDateArray2", "sDateArray");

        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest", "getStaticBufferWrap", "sCharArray");
        assertBug("MS_EXPOSE_BUF", "FindReturnRefTest", "getStaticBufferDuplicate", "sCharBuf");

        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticBuffer", "sCharBuf");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDate", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDate2", "sDate");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDateArray", "sDateArray");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticValues", "shm");
        assertBug("MS_EXPOSE_REP", "FindReturnRefTest", "getStaticDateArray2", "sDateArray");

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
