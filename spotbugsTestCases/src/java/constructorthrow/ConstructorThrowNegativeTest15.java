package constructorthrow;

/**
 * The isInitialized flag set by the constructor, and the exception is thrown by not the constructor depending on its value.
 */
public class ConstructorThrowNegativeTest15 {
    private volatile boolean isInitialized = false;

    public ConstructorThrowNegativeTest15() {
        if (!verify()) {
            return;
        }

        this.isInitialized = true;
    }

    private boolean verify() {
        return false;
    }

    public void doSomething () {
        if (!this.isInitialized) {
            throw new RuntimeException();
        }
        // do something
    }
}
