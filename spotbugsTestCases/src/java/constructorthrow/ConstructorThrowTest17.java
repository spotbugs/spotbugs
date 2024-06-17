package constructorthrow;

/**
 * The constructor throws an Exception conditionally.
 */
public class ConstructorThrowTest17 {
    public ConstructorThrowTest17() {
        if (!verify()) {
            throw new RuntimeException();
        }
    }

    private boolean verify() {
        return false;
    }
}
