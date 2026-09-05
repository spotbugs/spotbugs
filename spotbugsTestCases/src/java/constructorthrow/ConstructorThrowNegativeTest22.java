package constructorthrow;

import java.io.IOException;

/**
 * The Exception is thrown before the java.lang.Object constructor exits, so this is compliant, since in this case
 * the JVM will not execute an object's finalizer.
 */
public class ConstructorThrowNegativeTest22 extends SuperConstructorThrowNegativeTest22 {
    protected static boolean verify() throws IOException {
        // Returns true if data entered is valid, else throws an Exception
        // Assume that the attacker just enters invalid data, so this method always throws the exception
        throw new IOException("Still invalid data!");
    }

    public ConstructorThrowNegativeTest22() throws IOException {
        super();
    }
}

class SuperConstructorThrowNegativeTest22 {
    protected static boolean verify() throws IOException {
        // Returns true if data entered is valid, else throws an Exception
        // Assume that the attacker just enters invalid data, so this method always throws the exception
        throw new IOException("Invalid data!");
    }

    public SuperConstructorThrowNegativeTest22() throws IOException {
        this(verify());
    }

    private SuperConstructorThrowNegativeTest22(boolean secure) {
        // secure is always true
        // Constructor without any checks
    }
}
