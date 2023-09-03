package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

class FindIncreasedAccessibilityOfMethodsTest extends AbstractIntegrationTest {
    @Test
    void findIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromSamePackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/SubClassFromSamePackage.class");

        assertContainsCorrectNumberOfIAOMBugs(6);

        assertContainsIAOMBug("SubClassFromSamePackage", "protectedMethodToPublic");
        assertContainsIAOMBug("SubClassFromSamePackage", "packagePrivateMethodToPublic");
        assertContainsIAOMBug("SubClassFromSamePackage", "packagePrivateMethodToProtected");
        assertContainsIAOMBug("SubClassFromSamePackage", "superProtectedMethodToPublic");
        assertContainsIAOMBug("SubClassFromSamePackage", "superPackagePrivateMethodToPublic");
        assertContainsIAOMBug("SubClassFromSamePackage", "superPackagePrivateMethodToProtected");
    }

    @Test
    void findIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromAnotherPackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/anotherPackage/SubClassFromAnotherPackage.class");

        assertContainsCorrectNumberOfIAOMBugs(1);

        assertContainsIAOMBug("SubClassFromAnotherPackage", "protectedMethodToPublic");
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_CorrectSubClass() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/anotherPackage/CorrectSubClass.class");

        assertContainsCorrectNumberOfIAOMBugs(0);
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_CloneableImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/CloneableImplementation.class");

        assertContainsCorrectNumberOfIAOMBugs(0);
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_InterfaceImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/Interface.class",
                "increasedAccessibilityOfMethods/InterfaceImplementation.class");

        assertContainsCorrectNumberOfIAOMBugs(0);
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_GenericImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/GenericSuperClass.class",
                "increasedAccessibilityOfMethods/GenericSubClass.class");

        assertContainsCorrectNumberOfIAOMBugs(3);

        assertContainsIAOMBug("GenericSubClass", "protectedMethodToPublicWithGenericParameter");
        assertContainsIAOMBug("GenericSubClass", "packagePrivateMethodToPublicWithGenericParameter");
        assertContainsIAOMBug("GenericSubClass", "packagePrivateMethodToProtectedWithGenericParameter");
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassWithRepeatedOverride() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClassWithOverride.class",
                "increasedAccessibilityOfMethods/SubClassWithRepeatedOverride.class");

        assertContainsCorrectNumberOfIAOMBugs(3);

        assertContainsIAOMBug("SuperClassWithOverride", "superProtectedMethodToPublicOverriddenInSuperClass");
        assertContainsIAOMBug("SuperClassWithOverride", "superPackagePrivateMethodToProtectedOverriddenInSuperClass");
        assertContainsIAOMBug("SubClassWithRepeatedOverride", "superPackagePrivateMethodToProtectedOverriddenInSuperClass");
        assertNotContainsIAOMBug("SubClassWithRepeatedOverride", "superProtectedMethodToPublicOverriddenInSuperClass");
    }

    private void assertContainsCorrectNumberOfIAOMBugs(int numberOfBugs) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY").build();
        assertThat(getBugCollection(), containsExactly(numberOfBugs, bugTypeMatcher));
    }

    private void assertContainsIAOMBug(String className, String methodName) {
        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY")
                .inClass(className)
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertNotContainsIAOMBug(String className, String methodName) {
        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY")
                .inClass(className)
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), not(hasItem(bugInstanceMatcher)));
    }
}
