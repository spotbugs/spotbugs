package constructorthrow;

/**
 * However there is an unchecked exception thrown from the constructor, the superclass contains a final finalize.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2665">GitHub issue</a>
 */
public class ConstructorThrowNegativeTest13 extends ConstructorThrowNegativeTest12 {
    public ConstructorThrowNegativeTest13() {
        throw new RuntimeException(); // No error, final finalize.
    }
}
