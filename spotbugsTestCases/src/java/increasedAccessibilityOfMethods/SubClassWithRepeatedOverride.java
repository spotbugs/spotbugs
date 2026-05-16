package increasedAccessibilityOfMethods;

public class SubClassWithRepeatedOverride extends SuperClassWithOverride {
    public SubClassWithRepeatedOverride() {
        super();
    }

    @Override
    public String superPackagePrivateMethodToProtectedOverriddenInSuperClass() {
        return "SubClassWithRepeatedOverride.superPackagePrivateMethodToProtectedOverriddenInSuperClass";
    }

    @Override
    public String superProtectedMethodToPublicOverriddenInSuperClass() {
        return "SubClassWithRepeatedOverride.superProtectedMethodToPublicOverriddenInSuperClass";
    }
}
