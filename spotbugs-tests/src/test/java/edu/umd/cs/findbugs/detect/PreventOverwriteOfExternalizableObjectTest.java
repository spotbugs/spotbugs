package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class PreventOverwriteOfExternalizableObjectTest extends AbstractIntegrationTest {

    @Test
    void testBadReadExternal() {
        performAnalysis("externalizable/BadExternalizableTest.class");

        assertNumOfBugs(2);
        assertBug("BadExternalizableTest", 20);
        assertBug("BadExternalizableTest", 21);
    }

    @Test
    void testBadReadExternal2() {
        performAnalysis("externalizable/BadExternalizableTest2.class");

        assertNumOfBugs(1);
        assertBug("BadExternalizableTest2", 28);
    }

    @Test
    void testBadReadExternal3() {
        performAnalysis("externalizable/BadExternalizableTest3.class");

        assertNumOfBugs(1);
        assertBug("BadExternalizableTest3", 21);
    }

    @Test
    void testBadReadExternal4() {
        performAnalysis("externalizable/BadExternalizableTest4.class");

        assertNumOfBugs(1);
        assertBug("BadExternalizableTest4", 28);
    }

    @Test
    void testBadReadExternal5() {
        performAnalysis("externalizable/BadExternalizableTest5.class");

        assertNumOfBugs(2);
        assertBug("BadExternalizableTest5", 22);
        assertBug("BadExternalizableTest5", 23);
    }

    @Test
    void testBadReadExternal6() {
        performAnalysis("externalizable/BadExternalizableTest6.class");

        assertNumOfBugs(2);
        assertBug("BadExternalizableTest6", 22);
        assertBug("BadExternalizableTest6", 23);
    }

    @Test
    void testBadReadExternal7() {
        performAnalysis("externalizable/BadExternalizableTest7.class");

        assertNumOfBugs(2);
        assertBug("BadExternalizableTest7", 22);
        assertBug("BadExternalizableTest7", 23);
    }

    @Test
    void testGoodReadExternal() {
        performAnalysis("externalizable/GoodExternalizableTest.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal2() {
        performAnalysis("externalizable/GoodExternalizableTest2.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal3() {
        performAnalysis("externalizable/GoodExternalizableTest3.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal4() {
        performAnalysis("externalizable/GoodExternalizableTest4.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal5() {
        performAnalysis("externalizable/GoodExternalizableTest5.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal6() {
        performAnalysis("externalizable/GoodExternalizableTest6.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal7() {
        performAnalysis("externalizable/GoodExternalizableTest7.class");
        assertNumOfBugs(0);
    }

    @Test
    void testGoodReadExternal8() {
        performAnalysis("externalizable/GoodExternalizableTest8.class");
        assertNumOfBugs(0);
    }

    private void assertNumOfBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SE_PREVENT_EXT_OBJ_OVERWRITE").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug(String className, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("SE_PREVENT_EXT_OBJ_OVERWRITE")
                .inClass(className)
                .inMethod("readExternal")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
