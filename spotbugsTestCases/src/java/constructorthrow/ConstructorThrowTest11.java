package constructorthrow;

import java.io.IOException;

/**
 * In this test case, the constructor calls a method,
 * which calls a method, which throws an unchecked exception.
 */
public class ConstructorThrowTest11 {
    public ConstructorThrowTest11() {
        try {
            throw new IOException();
        } catch (IOException e) {
            System.err.println("IOException caught");
            throw new RuntimeException(e);
        }
    }
}
