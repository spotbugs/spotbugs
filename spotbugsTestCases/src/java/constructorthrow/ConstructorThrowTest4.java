package constructorthrow;

/**
 * In this test case, the constructor calls a method, that throws an
 * unchecked exception. The order of the method definitions are the
 * key in this test case, as the constructor is defined lastly.
 */
public class ConstructorThrowTest4 {
    public static void main(String[] args) {
        ConstructorThrowTest4 t = new ConstructorThrowTest4();
        t.testMethod();
        System.out.println("IO");
    }

    public void testMethod() {
        throw new RuntimeException();
    }

    public ConstructorThrowTest4() {
        testMethod();
    }
}
