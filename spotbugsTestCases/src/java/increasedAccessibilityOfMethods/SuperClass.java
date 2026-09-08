package increasedAccessibilityOfMethods;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SuperClass extends SuperClassOfSuperClass {
    protected SuperClass() {
        super();
    }

    public String publicMethod() {
        return "SuperClass.publicMethod";
    }

    protected String protectedMethodToPublic() {
        return "SuperClass.protectedMethodToPublic";
    }

    protected String protectedMethod() {
        return "SuperClass.protectedMethod";
    }

    String packagePrivateMethodToPublic() {
        return "SuperClass.packagePrivateMethodToPublic";
    }

    String packagePrivateMethodToProtected() {
        return "SuperClass.packagePrivateMethodToProtected";
    }

    String packagePrivate() {
        return "SuperClass.packagePrivate";
    }

    @SuppressFBWarnings("UPM")
    private String privateMethod() {
        return "SuperClass.privateMethod";
    }
}
