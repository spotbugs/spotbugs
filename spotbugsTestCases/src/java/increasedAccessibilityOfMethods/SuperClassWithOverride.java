package increasedAccessibilityOfMethods;

public class SuperClassWithOverride extends SuperClassOfSuperClass {
    protected SuperClassWithOverride() {
        super();
    }

    @Override
    protected String superPackagePrivateMethodToProtectedOverriddenInSuperClass() {
        return "SuperClassWithOverride.superPackagePrivateMethodToProtectedOverriddenInSuperClass";
    }

    @Override
    public String superProtectedMethodToPublicOverriddenInSuperClass() {
        return "SuperClassWithOverride.superProtectedMethodToPublicOverriddenInSuperClass";
    }
}
