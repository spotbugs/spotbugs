package constructorthrow;

/**
 * However the constructor throws an unchecked exception, there is an empty finalize in this class.
 */
public class ConstructorThrowNegativeTest12 {
    public ConstructorThrowNegativeTest12() {
        throw new RuntimeException(); // No error, final finalize.
    }

    @Override
    protected final void finalize(){}
}
