package constructorthrow;

/**
 * In this test case the constructor throws an unchecked exception,
 * there is no throws declaration.
 */
public class ConstructorThrowNegativeTest2 {

    public ConstructorThrowNegativeTest2() {
        throw new RuntimeException(); // No error, final finalize.
    }

    @Override
    protected final void finalize(){
        System.out.println("Do cleanup");
    }
}
