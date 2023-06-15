package edu.umd.cs.findbugs.detect;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class PreventOverwriteOfExternalizableObjectTest extends AbstractIntegrationTest {

    @Test
    public void testBadReadExternal() {
        performAnalysis("externalizable/BadExternalizableTest.class");

        assertNumOfBugs(2);
        assertBug("BadExternalizableTest", 20);
        assertBug("BadExternalizableTest", 21);
    }

    @Test
    public void testBadReadExternal2() {
        performAnalysis("externalizable/BadExternalizableTest2.class");

        assertNumOfBugs(1);
        assertBug("BadExternalizableTest2", 28);
    }

    @Test
    public void testBadReadExternal3() {
        performAnalysis("externalizable/BadExternalizableTest3.class");

        assertNumOfBugs(1);
        assertBug("BadExternalizableTest3", 21);
    }

    @Test
    public void testBadReadExternal4() {
        performAnalysis("externalizable/BadExternalizableTest4.class");

        assertNumOfBugs(1);
        assertBug("BadExternalizableTest4", 28);
    }

    @Test
    public void testBadReadExternal5() {
        performAnalysis("externalizable/BadExternalizableTest5.class");

        assertNumOfBugs(2);
        assertBug("BadExternalizableTest5", 22);
        assertBug("BadExternalizableTest5", 23);
    }

    @Test
    public void testGoodReadExternal() {
        performAnalysis("externalizable/GoodExternalizableTest.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testGoodReadExternal2() {
        performAnalysis("externalizable/GoodExternalizableTest2.class");

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
