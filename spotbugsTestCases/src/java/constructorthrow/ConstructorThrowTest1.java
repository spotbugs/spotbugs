package constructorthrow;

/**
 * In this test case the constructor throws an unchecked exception,
 * there is no throws declaration.
 */
public class ConstructorThrowTest1 {
    public ConstructorThrowTest1() {
        throw new RuntimeException(); // Error, constructor throw.
    }
}
