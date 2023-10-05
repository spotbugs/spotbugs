package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;


import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class DontReusePublicIdentifiersTest extends AbstractIntegrationTest {
    private static final String PI_CLASS_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES";
    private static final String PI_FIELD_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES";
    private static final String PI_METHOD_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES";
    private static final String PI_VARIABLE_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_LOCAL_VARIABLE_NAMES";

    @Test
    public void testShadowedPublicIdentifiersClassNames() {
        // check shadowed public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/InputStream.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_CLASS_BUG, 1);
        assertShadowedPublicIdentifierClassBug("InputStream");

        // check shadowed public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$Vector.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$Buffer.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$File.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames$BigInteger.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_CLASS_BUG, 4);
        assertShadowedPublicIdentifierClassBug("ShadowedPublicIdentifiersInnerClassNames$Vector");
        assertShadowedPublicIdentifierClassBug("ShadowedPublicIdentifiersInnerClassNames$Buffer");
        assertShadowedPublicIdentifierClassBug("ShadowedPublicIdentifiersInnerClassNames$File");
        assertShadowedPublicIdentifierClassBug("ShadowedPublicIdentifiersInnerClassNames$BigInteger");

        // check shadowed public identifiers as repeatedly nested classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2$List.class",
                "publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2$List$Node.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_CLASS_BUG, 2);
        assertShadowedPublicIdentifierClassBug("ShadowedPublicIdentifiersInnerClassNames2$List");
        assertShadowedPublicIdentifierClassBug("ShadowedPublicIdentifiersInnerClassNames2$List$Node");
    }

    @Test
    public void testGoodPublicIdentifiersClassNames() {
        // check good public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/GoodPublicIdentifiersStandaloneClassNames.class");
        assertZeroPublicIdentifierBug();

        // check good public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$1.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$MyBuffer.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$MyFile.class",
                "publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames$MyBigInteger.class");
        assertZeroPublicIdentifierBug();

        // check inner classes imported from the standard library
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames2.class");
        assertZeroPublicIdentifierBug();

        // check inner classes imported from other classes
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames3.class");
        assertZeroPublicIdentifierBug();
    }

    @Test
    public void testShadowedPublicIdentifiersFieldNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersFieldNames.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_FIELD_BUG, 2);
        assertShadowedPublicIdentifierFieldBug("String");
        assertShadowedPublicIdentifierFieldBug("Integer");
    }

    @Test
    public void testGoodPublicIdentifiersFieldNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersFieldNames.class");
        assertZeroPublicIdentifierBug();
    }

    @Test
    public void testShadowedPublicIdentifiersMethodNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersMethodNames.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_METHOD_BUG, 3);

        assertShadowedPublicIdentifierMethodBug("ShadowedPublicIdentifiersMethodNames", "InputStream", 5);
        assertShadowedPublicIdentifierMethodBug("ShadowedPublicIdentifiersMethodNames", "Integer", 9);
        assertShadowedPublicIdentifierMethodBug("ShadowedPublicIdentifiersMethodNames", "ArrayList", 14);
    }

    @Test
    public void testGoodPublicIdentifiersMethodNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersMethodNames.class");
        assertZeroPublicIdentifierBug();

        // check that the overridden method is detected only once inside superclass
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersOverriddenMethods.class",
                "publicIdentifiers/ShadowedPublicIdentifiersOverridableMethods.class");

        assertNumOfShadowedPublicIdentifierBugs(PI_METHOD_BUG, 1);
        assertShadowedPublicIdentifierMethodBug("ShadowedPublicIdentifiersOverridableMethods", "ArrayList", 5);
    }

    @Test
    public void testShadowedPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersVariableNames.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_VARIABLE_BUG, 3);

        assertShadowedPublicIdentifierVariableBug("containsShadowedLocalVariable1", "ArrayList");
        assertShadowedPublicIdentifierVariableBug("containsShadowedLocalVariable2", "Integer");
        assertShadowedPublicIdentifierVariableBug("containsShadowedLocalVariable3", "File");
    }

    @Test
    public void testGoodPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersVariableNames.class");
        assertZeroPublicIdentifierBug();
    }

    private void assertNumOfShadowedPublicIdentifierBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertZeroPublicIdentifierBug() {
        assertNumOfShadowedPublicIdentifierBugs(PI_CLASS_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_METHOD_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_FIELD_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_VARIABLE_BUG, 0);
    }

    private void assertShadowedPublicIdentifierClassBug(String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_CLASS_BUG)
                .inClass(className)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierFieldBug(String fieldName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_FIELD_BUG)
                .inClass("ShadowedPublicIdentifiersFieldNames")
                .atField(fieldName)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierMethodBug(String className, String methodName, int sourceLine) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_METHOD_BUG)
                .inClass(className)
                .inMethod(methodName)
                .atLine(sourceLine)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierVariableBug(String methodName, String variableName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_VARIABLE_BUG)
                .inClass("ShadowedPublicIdentifiersVariableNames")
                .inMethod(methodName)
                .atVariable(variableName)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
