package constructorthrow;

import java.io.IOException;

/**
 * In this test case, the constructor throws an IOException and catches it,
 * and in the catch block wraps it into a RuntimeException and throws it.
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
