package constructorthrow;

import java.io.IOException;

/**
 * In this test case, the constructor calls a method, which throws
 * an unchecked exception, but the constructor handles it.
 */
public class ConstructorThrowNegativeTest5 {
    public ConstructorThrowNegativeTest5() {
        try {
            testMethod();
        } catch (IOException e) {
            System.err.println("IOException caught, no error");
        }
    }

    public void testMethod() throws IOException {
        throw new IOException();
    }
}
