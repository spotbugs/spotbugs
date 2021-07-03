import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * In this testcase, the constructor calls an overloaded constructor, which calls
 * throws a method, which throws an unchecked exception.
 */
public class ConstructorThrowTest5{
    private int i = 0;

    public ConstructorThrowTest5() {
        this(5);
    }

    public ConstructorThrowTest5(int input) {
        i = input;
        testMethod(); 
    }
    
    @ExpectWarning("CT")
    public void testMethod() {
        System.out.println(i);
        throw new RuntimeException();
    }
}
