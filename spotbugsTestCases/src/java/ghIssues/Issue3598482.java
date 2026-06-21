package ghIssues;

/**
 * Regression for <a href="https://github.com/spotbugs/spotbugs/issues/3598482">#3598482</a>.
 */
public class Issue3598482 {

    public static void nullByteArrayInstanceof() {
        byte[] msg = null;
        if (msg instanceof byte[]) {
            System.out.println("byte array");
        }
    }
}
