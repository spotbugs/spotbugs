import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * In this testcase, the constructor calls a method, which throws
 * an unchecked exception.
 */
public class ConstructorThrowTest3{
    public ConstructorThrowTest3() {
        testMethod(); 
    }

    @ExpectWarning("CT")
    public void testMethod() {
        throw new RuntimeException();
    }
}
