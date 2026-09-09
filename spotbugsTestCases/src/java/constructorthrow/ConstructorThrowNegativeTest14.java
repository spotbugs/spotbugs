package constructorthrow;

/**
 * The Exception is thrown before the java.lang.Object constructor exits, so this is compliant, since in this case
 * the JVM will not execute an object's finalizer.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2710">GitHub issue</a>
 */
public class ConstructorThrowNegativeTest14 {
    public ConstructorThrowNegativeTest14() {
        this(verify());
    }

    private ConstructorThrowNegativeTest14(boolean secure) {
        // secure is always true
        // Constructor without any checks
    }

    private static boolean verify() {
        // Returns true if data entered is valid, else throws an Exception
        // Assume that the attacker just enters invalid data, so this method always throws the exception
        throw new RuntimeException("Invalid data!");
    }
}
