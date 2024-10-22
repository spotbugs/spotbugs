package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class DontReusePublicIdentifiersTest extends AbstractIntegrationTest {

    private static final String PI_CLASS_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES";
    private static final String PI_FIELD_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES";
    private static final String PI_METHOD_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES";
    private static final String PI_VARIABLE_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_LOCAL_VARIABLE_NAMES";

    @Test
    void testShadowedPublicIdentifiersClassNames() {
        // check shadowed public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/InputStream.class");
        assertBugTypeCount(PI_CLASS_BUG, 1);
        assertBugInClass(PI_CLASS_BUG, "InputStream");

        // check shadowed public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$Vector.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$Buffer.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$File.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$BigInteger.class");
        assertBugTypeCount(PI_CLASS_BUG, 4);
        assertBugInClass(PI_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames$Vector");
        assertBugInClass(PI_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames$Buffer");
        assertBugInClass(PI_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames$File");
        assertBugInClass(PI_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames$BigInteger");

        // check shadowed public identifiers as repeatedly nested classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2$List.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2$List$Node.class");
        assertBugTypeCount(PI_CLASS_BUG, 2);
        assertBugInClass(PI_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames2$List");
        assertBugInClass(PI_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames2$List$Node");
    }

    @Test
    void testGoodPublicIdentifiersClassNames() {
        // check good public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/GoodPublicIdentifiersStandaloneClassNames.class");
        assertZeroPublicIdentifierBug();

        // check good public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$MyBuffer.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$MyFile.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$MyBigInteger.class");
        assertZeroPublicIdentifierBug();

        // check inner classes imported from the standard library
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames2.class");
        assertZeroPublicIdentifierBug();

        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames3.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$BigInteger.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$Buffer.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$File.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$Vector.class");
        assertZeroPublicIdentifierBugInClass("GoodPublicIdentifiersInnerClassNames3");
    }

    @Test
    void testShadowedPublicIdentifiersFieldNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersFieldNames.class");
        assertBugTypeCount(PI_FIELD_BUG, 2);
        assertBugAtField(PI_FIELD_BUG, "ShadowedPublicIdentifiersFieldNames", "String");
        assertBugAtField(PI_FIELD_BUG, "ShadowedPublicIdentifiersFieldNames", "Integer");
    }

    @Test
    void testGoodPublicIdentifiersFieldNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersFieldNames.class");
        assertZeroPublicIdentifierBug();
    }

    @Test
    void testShadowedPublicIdentifiersMethodNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersMethodNames.class");
        assertBugTypeCount(PI_METHOD_BUG, 3);

        assertBugInMethodAtLine(PI_METHOD_BUG, "ShadowedPublicIdentifiersMethodNames", "InputStream", 5);
        assertBugInMethodAtLine(PI_METHOD_BUG, "ShadowedPublicIdentifiersMethodNames", "Integer", 9);
        assertBugInMethodAtLine(PI_METHOD_BUG, "ShadowedPublicIdentifiersMethodNames", "ArrayList", 14);
    }

    @Test
    void testGoodPublicIdentifiersMethodNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersMethodNames.class");
        assertZeroPublicIdentifierBug();

        // check that the overridden method is detected only once inside superclass
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersOverriddenMethods.class",
                "publicIdentifiers/ShadowedPublicIdentifiersOverridableMethods.class");

        assertBugTypeCount(PI_METHOD_BUG, 1);
        assertBugInMethodAtLine(PI_METHOD_BUG, "ShadowedPublicIdentifiersOverridableMethods", "ArrayList", 5);
    }

    @Test
    void testShadowedPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersVariableNames.class");
        assertBugTypeCount(PI_VARIABLE_BUG, 3);

        assertBugInMethodAtVariable(PI_VARIABLE_BUG, "ShadowedPublicIdentifiersVariableNames", "containsShadowedLocalVariable1", "ArrayList");
        assertBugInMethodAtVariable(PI_VARIABLE_BUG, "ShadowedPublicIdentifiersVariableNames", "containsShadowedLocalVariable2", "Integer");
        assertBugInMethodAtVariable(PI_VARIABLE_BUG, "ShadowedPublicIdentifiersVariableNames", "containsShadowedLocalVariable3", "File");
    }

    @Test
    void testGoodPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersVariableNames.class");
        assertZeroPublicIdentifierBug();
    }

    private void assertZeroPublicIdentifierBugInClass(String className) {
        final String[] bugTypes = new String[] { PI_CLASS_BUG, PI_FIELD_BUG, PI_METHOD_BUG, PI_VARIABLE_BUG };
        for (String bugType : bugTypes) {
            assertNoBugInClass(bugType, className);
        }
    }

    private void assertZeroPublicIdentifierBug() {
        assertNoBugType(PI_CLASS_BUG);
        assertNoBugType(PI_METHOD_BUG);
        assertNoBugType(PI_FIELD_BUG);
        assertNoBugType(PI_VARIABLE_BUG);
    }
}
