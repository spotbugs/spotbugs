import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * In this test case the constructor throws an unchecked exception,
 * there is no throws declaration.
 */
public final class ConstructorThrowNegativeTest1{
    
    @NoWarning("CT")
    public ConstructorThrowNegativeTest1() {
        throw new RuntimeException(); // No error, final class.
    }
}
