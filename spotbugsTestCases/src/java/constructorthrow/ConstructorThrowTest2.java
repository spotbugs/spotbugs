package constructorthrow;

import java.io.IOException;

/**
 * In this test case the constructor throws a checked exception,
 * there is a throws declaration.
 */
public class ConstructorThrowTest2 {
    public ConstructorThrowTest2() throws IOException {
        throw new IOException(); // Error, constructor throw.
    }
}
