package constructorthrow;

import java.io.IOException;

/**
 * Calling a static function with not void or primitive return value from the constructor,
 * which throws a checked exception.
 */
public class ConstructorThrowTest22 {
    public ConstructorThrowTest22() throws IOException {
        ConstructorThrowTest22.test();
    }

    private static String test() throws IOException {
        throw new IOException();
    }
}
