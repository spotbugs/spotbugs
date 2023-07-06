package constructorthrow;

/**
 * In this test case the constructor throws an unchecked exception,
 * there is no throws declaration, and there is a finalize method, that is
 * not overriding void#finalize(). A Warning still should be emited.
 * Inspired by @KengoToda's suggestion.
 */
public class ConstructorThrowTest6 {
    public ConstructorThrowTest6() {
        throw new RuntimeException(); // Error, constructor throw.
    }

    final void finalize(Object object) {
        System.out.println("I am not overriding the void#finalize() method");
    }
}
