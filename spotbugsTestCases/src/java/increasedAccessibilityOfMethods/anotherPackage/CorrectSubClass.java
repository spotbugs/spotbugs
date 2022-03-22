package increasedAccessibilityOfMethods.anotherPackage;

import increasedAccessibilityOfMethods.SuperClass;

public class CorrectSubClass extends SuperClass {

    public String packagePrivate() {
        return "CorrectSubClass.packagePrivate";
    }

    public String privateMethod() {
        return "CorrectSubClass.privateMethod";
    }

}
