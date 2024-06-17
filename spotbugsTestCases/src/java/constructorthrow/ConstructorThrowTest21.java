package constructorthrow;

/**
 * Calling a static function with not void or primitive return value from the constructor,
 * which throws an unchecked exception.
 */
public class ConstructorThrowTest21 {
    public ConstructorThrowTest21() {
        ConstructorThrowTest21.test();
    }

    private static String test() {
        throw new IllegalStateException();
    }
}
