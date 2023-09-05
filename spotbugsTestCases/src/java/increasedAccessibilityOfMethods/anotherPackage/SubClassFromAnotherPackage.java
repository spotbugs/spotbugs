package increasedAccessibilityOfMethods.anotherPackage;

import increasedAccessibilityOfMethods.SuperClass;

public class SubClassFromAnotherPackage extends SuperClass {

    public SubClassFromAnotherPackage() {
        super();
    }

    @Override
    public String protectedMethodToPublic() {
        return "SubClassFromAnotherPackage.protectedMethodToPublic";
    }

}
