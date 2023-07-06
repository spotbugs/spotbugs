package constructorthrow;

/**
 * In this test case, the constructor calls a method, which throws
 * an unchecked exception.
 */
public class ConstructorThrowTest3 {
    public ConstructorThrowTest3() {
        testMethod();
    }

    public void testMethod() {
        throw new RuntimeException();
    }
}
