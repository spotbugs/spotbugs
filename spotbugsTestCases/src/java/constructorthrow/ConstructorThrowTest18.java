package constructorthrow;

import java.io.IOException;

/**
 * The constructor throws an Exception conditionally.
 */
public class ConstructorThrowTest18 {
    public ConstructorThrowTest18() throws IOException {
        if (!verify()) {
            throw new IOException();
        }
    }

    private boolean verify() {
        return false;
    }
}
