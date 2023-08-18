package constructorthrow;

/**
 * In this test case, the constructor calls a method, that throws an unchecked exception,
 * and also directly throws an unchecked exception. So there are several problems in the same constructor.
 * The error marker is at the first problematic line.
 */
public class ConstructorThrowTest16 {
    public ConstructorThrowTest16() {
        testMethod();
        throw new RuntimeException();
    }

    public void testMethod() {
        throw new RuntimeException();
    }
}
