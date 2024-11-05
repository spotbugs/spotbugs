package constructorthrow;

/**
 * In this test case, the constructor calls a method,
 * which calls a method, which throws an unchecked exception.
 */
public class ConstructorThrowTest10 {
    public ConstructorThrowTest10() {
        wrapperMethod();
    }

    public void wrapperMethod() {
        testMethod();
    }

    public void testMethod() {
        throw new RuntimeException();
    }
}
