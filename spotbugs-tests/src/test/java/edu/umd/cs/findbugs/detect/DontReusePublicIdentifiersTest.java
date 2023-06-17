package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import java.util.List;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class DontReusePublicIdentifiersTest extends AbstractIntegrationTest {
    private static final String PI_CLASS_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES";
    private static final String PI_INNER_CLASS_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_INNER_CLASS_NAMES";
    private static final String PI_FIELD_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES";
    private static final String PI_METHOD_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES";
    private static final String PI_VARIABLE_BUG = "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_LOCAL_VARIABLE_NAMES";

    @Test
    public void testShadowedPublicIdentifiersClassNames() {
        // check shadowed public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/InputStream.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_CLASS_BUG, 1);
        assertShadowedPublicIdentifierClassBug();

        // check shadowed public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_INNER_CLASS_BUG, 4);
        assertShadowedPublicIdentifierInnerClassBug("ShadowedPublicIdentifiersInnerClassNames", "Vector");
        assertShadowedPublicIdentifierInnerClassBug("ShadowedPublicIdentifiersInnerClassNames", "File");
        assertShadowedPublicIdentifierInnerClassBug("ShadowedPublicIdentifiersInnerClassNames", "Buffer");
        assertShadowedPublicIdentifierInnerClassBug("ShadowedPublicIdentifiersInnerClassNames", "BigInteger");

        // check shadowed public identifiers as repeatedly nested classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames2.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_INNER_CLASS_BUG, 2);
        assertShadowedPublicIdentifierInnerClassBug("ShadowedPublicIdentifiersInnerClassNames2", "List");
        assertShadowedPublicIdentifierInnerClassBug("ShadowedPublicIdentifiersInnerClassNames2", "Node");
    }

    @Test
    public void testGoodPublicIdentifiersClassNames() {
        // check good public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/GoodPublicIdentifiersStandaloneClassNames.class");
        assertZeroPublicIdentifierBug();

        // check good public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames.class");
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

        assertShadowedPublicIdentifierMethodBug("InputStream", 5);
        assertShadowedPublicIdentifierMethodBug("Integer", 9);
        assertShadowedPublicIdentifierMethodBug("ArrayList", 14);
    }

    @Test
    public void testGoodPublicIdentifiersMethodNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersMethodNames.class",
                "publicIdentifiers/ShadowedPublicIdentifiersOverriddenMethods.class");
        assertZeroPublicIdentifierBug();
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
        assertNumOfShadowedPublicIdentifierBugs(PI_INNER_CLASS_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_METHOD_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_FIELD_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_VARIABLE_BUG, 0);
    }

    private void assertShadowedPublicIdentifierClassBug() {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_CLASS_BUG)
                .inClass("InputStream")
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

    private void assertShadowedPublicIdentifierMethodBug(String methodName, int sourceLine) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_METHOD_BUG)
                .inClass("ShadowedPublicIdentifiersMethodNames")
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

    private void assertShadowedPublicIdentifierInnerClassBug(String applicationClassName, String innerClassName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(PI_INNER_CLASS_BUG)
                .inClass(applicationClassName)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));


        final ClassAnnotation innerClassAnnotation = new ClassAnnotation(innerClassName);
        List<ClassAnnotation> bugAnnotations = getBugCollection().getCollection().stream()
                .flatMap(bugInstance -> bugInstance.getAnnotations().stream())
                .filter(bugAnnotation -> innerClassAnnotation.getClass().equals(bugAnnotation.getClass()))
                .map(bugAnnotation -> (ClassAnnotation) bugAnnotation)
                .toList();
        assertThat(bugAnnotations, hasItem(innerClassAnnotation));
    }
}
