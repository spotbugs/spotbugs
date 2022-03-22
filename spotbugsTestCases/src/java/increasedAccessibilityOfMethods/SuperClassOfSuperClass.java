package increasedAccessibilityOfMethods;

public class SuperClassOfSuperClass {

    public String superPublicMethod() {
        return "SuperClassOfSuperClass.superPublicMethod";
    }

    protected String superProtectedMethodToPublic() {
        return "SuperClassOfSuperClass.superProtectedMethodToPublic";
    }

    protected String superProtectedMethod() {
        return "SuperClassOfSuperClass.superProtectedMethod";
    }

    String superPackagePrivateMethodToPublic() {
        return "SuperClassOfSuperClass.superPackagePrivateMethodToPublic";
    }

    String superPackagePrivateMethodToProtected() {
        return "SuperClassOfSuperClass.superPackagePrivateMethodToProtected";
    }

    String superPackagePrivate() {
        return "SuperClassOfSuperClass.superPackagePrivate";
    }

}
