package increasedAccessibilityOfMethods;

public class GenericSubClass<K> extends GenericSuperClass<K> {

    @Override
    public String publicMethodWithGenericParameter(K parameter) {
        return "GenericSubClass.publicMethodWithGenericParameter";
    }

    @Override
    public String protectedMethodToPublicWithGenericParameter(K parameter) {
        return "GenericSubClass.protectedMethodToPublicWithGenericParameter";
    }

    @Override
    protected String protectedMethodWithGenericParameter(K parameter) {
        return "GenericSubClass.protectedMethodWithGenericParameter";
    }

    @Override
    public String packagePrivateMethodToPublicWithGenericParameter(K parameter) {
        return "GenericSubClass.packagePrivateMethodToPublicWithGenericParameter";
    }

    @Override
    protected String packagePrivateMethodToProtectedWithGenericParameter(K parameter) {
        return "GenericSubClass.packagePrivateMethodToProtectedWithGenericParameter";
    }

    @Override
    String packagePrivateWithGenericParameter(K parameter) {
        return "GenericSubClass.packagePrivateWithGenericParameter";
    }

}
