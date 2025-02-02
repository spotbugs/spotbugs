package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class PreventOverwriteOfExternalizableObjectTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "SE_PREVENT_EXT_OBJ_OVERWRITE";

    @Test
    void testBadReadExternal() {
        performAnalysis("externalizable/BadExternalizableTest.class");

        assertBugTypeCount(BUG_TYPE, 2);
        assertBug("BadExternalizableTest", 20);
        assertBug("BadExternalizableTest", 21);
    }

    @Test
    void testBadReadExternal2() {
        performAnalysis("externalizable/BadExternalizableTest2.class");

        assertBugTypeCount(BUG_TYPE, 1);
        assertBug("BadExternalizableTest2", 28);
    }

    @Test
    void testBadReadExternal3() {
        performAnalysis("externalizable/BadExternalizableTest3.class");

        assertBugTypeCount(BUG_TYPE, 1);
        assertBug("BadExternalizableTest3", 21);
    }

    @Test
    void testBadReadExternal4() {
        performAnalysis("externalizable/BadExternalizableTest4.class");

        assertBugTypeCount(BUG_TYPE, 1);
        assertBug("BadExternalizableTest4", 28);
    }

    @Test
    void testBadReadExternal5() {
        performAnalysis("externalizable/BadExternalizableTest5.class");

        assertBugTypeCount(BUG_TYPE, 2);
        assertBug("BadExternalizableTest5", 22);
        assertBug("BadExternalizableTest5", 23);
    }

    @Test
    void testBadReadExternal6() {
        performAnalysis("externalizable/BadExternalizableTest6.class");

        assertBugTypeCount(BUG_TYPE, 2);
        assertBug("BadExternalizableTest6", 22);
        assertBug("BadExternalizableTest6", 23);
    }

    @Test
    void testBadReadExternal7() {
        performAnalysis("externalizable/BadExternalizableTest7.class");

        assertBugTypeCount(BUG_TYPE, 2);
        assertBug("BadExternalizableTest7", 22);
        assertBug("BadExternalizableTest7", 23);
    }

    @Test
    void testGoodReadExternal() {
        performAnalysis("externalizable/GoodExternalizableTest.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal2() {
        performAnalysis("externalizable/GoodExternalizableTest2.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal3() {
        performAnalysis("externalizable/GoodExternalizableTest3.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal4() {
        performAnalysis("externalizable/GoodExternalizableTest4.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal5() {
        performAnalysis("externalizable/GoodExternalizableTest5.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal6() {
        performAnalysis("externalizable/GoodExternalizableTest6.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal7() {
        performAnalysis("externalizable/GoodExternalizableTest7.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodReadExternal8() {
        performAnalysis("externalizable/GoodExternalizableTest8.class");
        assertNoBugType(BUG_TYPE);
    }

    private void assertBug(String className, int line) {
        assertBugInMethodAtLine(BUG_TYPE, className, "readExternal", line);
    }
}
