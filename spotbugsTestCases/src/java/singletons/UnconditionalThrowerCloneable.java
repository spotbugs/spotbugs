package singletons;

public class UnconditionalThrowerCloneable {
    private static UnconditionalThrowerCloneable instance;

    private UnconditionalThrowerCloneable() {
        // Private constructor prevents instantiation by untrusted callers
    }

    // Lazy initialization
    public static synchronized UnconditionalThrowerCloneable getInstance() {
        if (instance == null) {
            instance = new UnconditionalThrowerCloneable();
        }
        return instance;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
