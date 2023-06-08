package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class TestASMTest extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("TestASM.class");

        assertNumOfBugs(1, "NM_METHOD_NAMING_CONVENTION");
        assertNumOfBugs(6, "NM_FIELD_NAMING_CONVENTION");
        // It is reported both by TestASM and IDivResultCastToDouble
        assertNumOfBugs(2, "ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL");

        assertMethodNamingBug("BadMethodName");
        assertFieldNamingBug("badFieldNamePublicStaticFinal");
        assertFieldNamingBug("BadFieldNamePublicStatic");
        assertFieldNamingBug("BadFieldNamePublic");
        assertFieldNamingBug("BadFieldNameProtectedStatic");
        assertFieldNamingBug("BadFieldNameProtected");
        assertFieldNamingBug("BadFieldNamePrivate");
        assertMethodNamingBug("BadMethodName");
        assertCastBug("BadMethodName");
    }

    private void assertNumOfBugs(int num, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertMethodNamingBug(String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("NM_METHOD_NAMING_CONVENTION")
                .inClass("TestASM")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertFieldNamingBug(String field) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("NM_FIELD_NAMING_CONVENTION")
                .inClass("TestASM")
                .atField(field)
                .build();
        BugCollection bc = getBugCollection();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertCastBug(String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL")
                .inClass("TestASM")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
