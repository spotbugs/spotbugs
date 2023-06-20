package constructorthrow;

/**
 * In this test case the constructor throws an unchecked exception,
 * there is no throws declaration.
 */
public final class ConstructorThrowNegativeTest1 {
    public ConstructorThrowNegativeTest1() {
        throw new RuntimeException(); // No error, final class.
    }
}
