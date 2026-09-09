package constructorthrow;

/**
 * In this test case, the constructor calls a method, which in which an
 * exception is thrown and handled.
 */
public class ConstructorThrowNegativeTest4 {
    public ConstructorThrowNegativeTest4() {
        testMethodWrapperNoException();
    }

    public void testMethodWrapperNoException() {
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
