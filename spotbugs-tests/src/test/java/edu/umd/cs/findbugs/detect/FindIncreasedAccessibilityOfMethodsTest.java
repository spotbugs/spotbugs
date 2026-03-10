package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindIncreasedAccessibilityOfMethodsTest extends AbstractIntegrationTest {
    private static final String BUGTYPE = "IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY";

    @Test
    void findIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromSamePackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/SubClassFromSamePackage.class");

        assertBugTypeCount(BUGTYPE, 6);

        assertBugInMethod(BUGTYPE, "SubClassFromSamePackage", "protectedMethodToPublic");
        assertBugInMethod(BUGTYPE, "SubClassFromSamePackage", "packagePrivateMethodToPublic");
        assertBugInMethod(BUGTYPE, "SubClassFromSamePackage", "packagePrivateMethodToProtected");
        assertBugInMethod(BUGTYPE, "SubClassFromSamePackage", "superProtectedMethodToPublic");
        assertBugInMethod(BUGTYPE, "SubClassFromSamePackage", "superPackagePrivateMethodToPublic");
        assertBugInMethod(BUGTYPE, "SubClassFromSamePackage", "superPackagePrivateMethodToProtected");
    }

    @Test
    void findIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassFromAnotherPackage() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClass.class",
                "increasedAccessibilityOfMethods/anotherPackage/SubClassFromAnotherPackage.class");

        assertBugTypeCount(BUGTYPE, 1);

        assertBugInMethod(BUGTYPE, "SubClassFromAnotherPackage", "protectedMethodToPublic");
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_CorrectSubClass() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/anotherPackage/CorrectSubClass.class");

        assertNoBugType(BUGTYPE);
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_CloneableImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/CloneableImplementation.class");

        assertNoBugType(BUGTYPE);
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_InterfaceImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/Interface.class",
                "increasedAccessibilityOfMethods/InterfaceImplementation.class");

        assertNoBugType(BUGTYPE);
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_GenericImplementation() {
        performAnalysis(
                "increasedAccessibilityOfMethods/GenericSuperClass.class",
                "increasedAccessibilityOfMethods/GenericSubClass.class");

        assertBugTypeCount(BUGTYPE, 3);

        assertBugInMethod(BUGTYPE, "GenericSubClass", "protectedMethodToPublicWithGenericParameter");
        assertBugInMethod(BUGTYPE, "GenericSubClass", "packagePrivateMethodToPublicWithGenericParameter");
        assertBugInMethod(BUGTYPE, "GenericSubClass", "packagePrivateMethodToProtectedWithGenericParameter");
    }

    @Test
    void findNoIAOMBugInClass_IncreasedAccessibilityOfMethods_SubClassWithRepeatedOverride() {
        performAnalysis(
                "increasedAccessibilityOfMethods/SuperClassOfSuperClass.class",
                "increasedAccessibilityOfMethods/SuperClassWithOverride.class",
                "increasedAccessibilityOfMethods/SubClassWithRepeatedOverride.class");

        assertBugTypeCount(BUGTYPE, 3);

        assertBugInMethod(BUGTYPE, "SuperClassWithOverride", "superProtectedMethodToPublicOverriddenInSuperClass");
        assertBugInMethod(BUGTYPE, "SuperClassWithOverride", "superPackagePrivateMethodToProtectedOverriddenInSuperClass");
        assertBugInMethod(BUGTYPE, "SubClassWithRepeatedOverride", "superPackagePrivateMethodToProtectedOverriddenInSuperClass");
        assertNoBugInMethod(BUGTYPE, "SubClassWithRepeatedOverride", "superProtectedMethodToPublicOverriddenInSuperClass");
    }
}
