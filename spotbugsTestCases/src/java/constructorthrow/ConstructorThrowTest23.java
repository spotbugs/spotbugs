package constructorthrow;

import java.util.function.Supplier;

/**
 * The lambda which throws the exception is created and assigned to a field, and used in the Constructor.
 * It's similar to {@link ConstructorThrowNegativeTest19}, but it throws an exception.
 */
public class ConstructorThrowTest23 {
    Supplier<String> s;

    public ConstructorThrowTest23() {
        s = ConstructorThrowTest23::supplier;
        String str = s.get();
    }

    private static String supplier() {
        throw new IllegalStateException();
    }
}
