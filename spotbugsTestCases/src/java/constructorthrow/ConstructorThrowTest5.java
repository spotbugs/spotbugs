package constructorthrow;

/**
 * In this test case, the constructor calls an overloaded constructor,
 * which calls a method, which throws an unchecked exception.
 */
public class ConstructorThrowTest5 {
    private int i = 0;

    public ConstructorThrowTest5() {
        this(5);
    }

    public ConstructorThrowTest5(int input) {
        i = input;
        testMethod();
    }

    public void testMethod() {
        System.out.println(i);
        throw new RuntimeException();
    }
}
