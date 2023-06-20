package constructorthrow;

/**
 * In this test case, the constructor calls a method, which throws
 * an unchecked exception, but the constructor handles it.
 */
public class ConstructorThrowNegativeTest3 {
    public ConstructorThrowNegativeTest3() {
        try {
            testMethod();
        } catch (RuntimeException e) {
            System.err.println("Exception caught, no error");
        }
    }

    public void testMethod() {
        throw new RuntimeException();
    }
}
