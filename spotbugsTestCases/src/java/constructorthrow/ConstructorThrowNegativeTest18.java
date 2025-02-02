package constructorthrow;

import java.io.IOException;

/**
 * The Exception is thrown before the java.lang.Object constructor exits, so this is compliant, since in this case
 * the JVM will not execute an object's finalizer.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2710">GitHub issue</a>
 */
public class ConstructorThrowNegativeTest18 {
    public ConstructorThrowNegativeTest18() throws IOException {
        this(verify());
    }

    private ConstructorThrowNegativeTest18(boolean secure) {
        // secure is always true
        // Constructor without any checks
    }

    private static boolean verify() throws IOException {
        // Returns true if data entered is valid, else throws an Exception
        // Assume that the attacker just enters invalid data, so this method always throws the exception
        throw new IOException("Invalid data!");
    }
}
