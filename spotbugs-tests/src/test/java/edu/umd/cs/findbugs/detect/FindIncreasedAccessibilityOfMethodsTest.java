package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindIncreasedAccessibilityOfMethodsTest extends AbstractIntegrationTest {

    @Test
    public void findIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromSamePackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/SubClassFromSamePackage.class");

        assertNumberOfIAOMBugs(6);

        assertIAOMBug("SubClassFromSamePackage", "protectedMethodToPublic");
        assertIAOMBug("SubClassFromSamePackage", "packagePrivateMethodToPublic");
        assertIAOMBug("SubClassFromSamePackage", "packagePrivateMethodToProtected");
        assertIAOMBug("SubClassFromSamePackage", "superProtectedMethodToPublic");
        assertIAOMBug("SubClassFromSamePackage", "superPackagePrivateMethodToPublic");
        assertIAOMBug("SubClassFromSamePackage", "superPackagePrivateMethodToProtected");
    }

    @Test
    public void findIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromAnotherPackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/anotherPackage/SubClassFromAnotherPackage.class");

        assertNumberOfIAOMBugs(1);

        assertIAOMBug("SubClassFromAnotherPackage", "protectedMethodToPublic");
    }

    @Test
    public void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromAnotherPackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/anotherPackage/CorrectSubClass.class");

        assertNumberOfIAOMBugs(0);
    }

    @Test
    public void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_CloneableImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/CloneableImplementation.class");

        assertNumberOfIAOMBugs(0);
    }

    @Test
    public void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_InterfaceImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/Interface.class",
                "increasedAccessibilityOfMethods/InterfaceImplementation.class");

        assertNumberOfIAOMBugs(0);
    }

    @Test
    public void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_GenericImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/GenericSuperClass.class",
                "increasedAccessibilityOfMethods/GenericSubClass.class");

        assertNumberOfIAOMBugs(3);

        assertIAOMBug("GenericSubClass", "protectedMethodToPublicWithGenericParameter");
        assertIAOMBug("GenericSubClass", "packagePrivateMethodToPublicWithGenericParameter");
        assertIAOMBug("GenericSubClass", "packagePrivateMethodToProtectedWithGenericParameter");
    }

    private void assertNumberOfIAOMBugs(int numberOfBugs) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY").build();
        assertThat(getBugCollection(), containsExactly(numberOfBugs, bugTypeMatcher));
    }

    private void assertIAOMBug(String className, String methodName) {
        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY")
                .inClass(className)
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
