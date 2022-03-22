package increasedAccessibilityOfMethods;

public class SubClassFromSamePackage extends SuperClass {

    public SubClassFromSamePackage() {
        super();
    }

    @Override
    public String protectedMethodToPublic() {
        return "SubClassFromSamePackage.protectedMethodToPublic";
    }

    @Override
    public String packagePrivateMethodToPublic() {
        return "SubClassFromSamePackage.packagePrivateMethodToPublic";
    }

    @Override
    protected String packagePrivateMethodToProtected() {
        return "SubClassFromSamePackage.packagePrivateMethodToProtected";
    }

    @Override
    public String superProtectedMethodToPublic() {
        return "SubClassFromSamePackage.superProtectedMethodToPublic";
    }

    @Override
    public String superPackagePrivateMethodToPublic() {
        return "SubClassFromSamePackage.superPackagePrivateMethodToPublic";
    }

    @Override
    protected String superPackagePrivateMethodToProtected() {
        return "SubClassFromSamePackage.superPackagePrivateMethodToProtected";
    }

}
