package constructorthrow;

/**
 * The lambda which throws the exception is created and assigned to a field, and used in the Constructor.
 * It's similar to {@link ConstructorThrowNegativeTest19}, but it throws an exception.
 */
public class ConstructorThrowTest24 {
    String s;

    public ConstructorThrowTest24() {
        s = ConstructorThrowTest24.supplier();
    }

    private static String supplier() {
        throw new IllegalStateException();
    }
}
