import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * In this test case the constructor throws an unchecked exception,
 * there is no throws declaration.
 */
public class ConstructorThrowTest1{
    @ExpectWarning("CT")
    public ConstructorThrowTest1() {
        throw new RuntimeException(); // Error, constructor throw.
    }
}
