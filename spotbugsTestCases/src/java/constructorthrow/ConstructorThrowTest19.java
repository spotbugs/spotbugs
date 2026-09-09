package constructorthrow;

import java.io.IOException;

/**
 * The constructor throws an Exception conditionally.
 */
public class ConstructorThrowTest19 {
    public ConstructorThrowTest19() throws IOException {
        if (!verify()) {
            throwEx();
        }
    }

    private void throwEx() throws IOException {
        throw new IOException();
    }

    private boolean verify() {
        return false;
    }
}
