package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.filter.NameMatch;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import java.util.List;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;

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
        assertShadowedPublicIdentifierClassBug(PI_CLASS_BUG, "InputStream");

        // check shadowed public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/ShadowedPublicIdentifiersInnerClassNames.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_INNER_CLASS_BUG, 4);
        assertShadowedPublicIdentifierInnerClassBug("Vector");
        assertShadowedPublicIdentifierInnerClassBug("File");
        assertShadowedPublicIdentifierInnerClassBug("Buffer");
        assertShadowedPublicIdentifierInnerClassBug("BigInteger");
    }

    @Test
    public void testGoodPublicIdentifiersClassNames() {
        // check good public identifiers as standalone classes
        performAnalysis("publicIdentifiers/standalone/GoodPublicIdentifiersStandaloneClassNames.class");
        // assertDoesNotContainPublicIdentifierBugs();
        assertZeroPublicIdentifierBug();

        // check good public identifiers as inner classes
        performAnalysis("publicIdentifiers/inner/GoodPublicIdentifiersInnerClassNames.class");
        // assertDoesNotContainPublicIdentifierBugs();
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

        // NOTE: BUG there is a 2-3 line offset in detected bug:
        //  addSourceLine(this)
        //  addSourceLine(context, start, end) etc
        //  lineNumberTable is not returning the correct line number
        assertShadowedPublicIdentifierMethodBug(PI_METHOD_BUG, "ShadowedPublicIdentifiersMethodNames", "InputStream", 5);
        assertShadowedPublicIdentifierMethodBug(PI_METHOD_BUG, "ShadowedPublicIdentifiersMethodNames", "Integer", 10);
        assertShadowedPublicIdentifierMethodBug(PI_METHOD_BUG, "ShadowedPublicIdentifiersMethodNames", "ArrayList", 14);
    }

    @Test
    public void testGoodPublicIdentifiersMethodNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersMethodNames.class");
        assertZeroPublicIdentifierBug();
    }

    @Test
    public void testShadowedPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/ShadowedPublicIdentifiersVariableNames.class");
        assertNumOfShadowedPublicIdentifierBugs(PI_VARIABLE_BUG, 3);

        // NOTE: BUG there is a 2-3 line offset in detected bug:
        //  addSourceLine(this)
        //  addSourceLine(context, start, end) etc
        //  lineNumberTable is not returning the correct line number
        assertShadowedPublicIdentifierVariableBug("containsShadowedLocalVariable1", "ArrayList", 11);
        assertShadowedPublicIdentifierVariableBug("containsShadowedLocalVariable2", "Integer", 17);
        assertShadowedPublicIdentifierVariableBug("containsShadowedLocalVariable3", "File", 21);
    }

    @Test
    public void testGoodPublicIdentifiersVariableNames() {
        performAnalysis("publicIdentifiers/GoodPublicIdentifiersVariableNames.class");
        assertZeroPublicIdentifierBug();
    }

    private void assertNumOfShadowedPublicIdentifierBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertZeroPublicIdentifierBug() {
        assertNumOfShadowedPublicIdentifierBugs(PI_CLASS_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_INNER_CLASS_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_METHOD_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_FIELD_BUG, 0);
        assertNumOfShadowedPublicIdentifierBugs(PI_VARIABLE_BUG, 0);
    }

    private void assertShadowedPublicIdentifierClassBug(String bugType, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType(bugType).inClass(className).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierFieldBug(String fieldName) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType(DontReusePublicIdentifiersTest.PI_FIELD_BUG).inClass(
                "ShadowedPublicIdentifiersFieldNames").atField(fieldName).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierMethodBug(String bugType, String className, String methodName, int sourceLine) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType(bugType).inClass(className).inMethod(methodName).atLine(
                sourceLine).build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertShadowedPublicIdentifierVariableBug(String methodName, String variableName, int sourceLine) {
        assertShadowedPublicIdentifierMethodBug(DontReusePublicIdentifiersTest.PI_VARIABLE_BUG, "ShadowedPublicIdentifiersVariableNames", methodName,
                sourceLine);

        NameMatch variableNameMatcher = new NameMatch(variableName);

        List<String> variableNameAnnotations = getBugCollection().getCollection().stream().map(bugInstance -> (StringAnnotation) bugInstance
                .getAnnotations().get(3)).map(StringAnnotation::getValue).toList();

        assertTrue(variableNameAnnotations.stream().anyMatch(variableNameMatcher::match));
    }

    private void assertShadowedPublicIdentifierInnerClassBug(String innerClassName) {
        assertShadowedPublicIdentifierClassBug(DontReusePublicIdentifiersTest.PI_INNER_CLASS_BUG, "ShadowedPublicIdentifiersInnerClassNames");

        NameMatch innerClassMatcher = new NameMatch(innerClassName);

        List<String> bugAnnotations = getBugCollection().getCollection().stream().map(bugInstance -> (ClassAnnotation) bugInstance.getAnnotations()
                .get(1)).map(classAnnotation -> classAnnotation.format("simpleClass", classAnnotation)).toList();

        assertTrue(bugAnnotations.stream().anyMatch(innerClassMatcher::match));
    }
}
