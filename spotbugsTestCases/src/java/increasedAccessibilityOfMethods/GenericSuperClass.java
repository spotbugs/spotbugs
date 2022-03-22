package increasedAccessibilityOfMethods;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class GenericSuperClass<T> {

    public String publicMethodWithGenericParameter(T parameter) {
        return "GenericSuperClass.publicMethodWithGenericParameter";
    }

    protected String protectedMethodToPublicWithGenericParameter(T parameter) {
        return "GenericSuperClass.protectedMethodToPublicWithGenericParameter";
    }

    protected String protectedMethodWithGenericParameter(T parameter) {
        return "GenericSuperClass.protectedMethodWithGenericParameter";
    }

    String packagePrivateMethodToPublicWithGenericParameter(T parameter) {
        return "GenericSuperClass.packagePrivateMethodToPublicWithGenericParameter";
    }

    String packagePrivateMethodToProtectedWithGenericParameter(T parameter) {
        return "GenericSuperClass.packagePrivateMethodToProtectedWithGenericParameter";
    }

    String packagePrivateWithGenericParameter(T parameter) {
        return "GenericSuperClass.packagePrivateWithGenericParameter";
    }

    @SuppressFBWarnings("UPM")
    private String privateMethodWithGenericParameter(T parameter) {
        return "GenericSuperClass.privateMethodWithGenericParameter";
    }

}
