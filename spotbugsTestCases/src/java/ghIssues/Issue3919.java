package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3919">#3919</a>.
 */
public class Issue3919 {
    static class RuntimeExceptionTypeA extends RuntimeException {
        RuntimeExceptionTypeA(Throwable cause) {
            super(cause);
        }
    }

    static class RuntimeExceptionTypeB extends RuntimeException {
        RuntimeExceptionTypeB(Throwable cause) {
            super(cause);
        }
    }

    static class SpecificCheckedException extends Exception {
        SpecificCheckedException() {
            super();
        }
    }

    private boolean someSpecificState;

    @NoWarning("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public void throwViaHelper() {
        try {
            doSomething();
        } catch (SpecificCheckedException ex) {
            throw mapException(ex);
        }
    }

    @NoWarning("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public void throwInlineSubtypes() {
        try {
            doSomething();
        } catch (SpecificCheckedException ex) {
            if (someSpecificState) {
                throw new RuntimeExceptionTypeA(ex);
            } else {
                throw new RuntimeExceptionTypeB(ex);
            }
        }
    }

    @ExpectWarning("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public void throwDirectRuntimeException() {
        throw new RuntimeException();
    }

    private void doSomething() throws SpecificCheckedException {
        throw new SpecificCheckedException();
    }

    private RuntimeException mapException(Exception ex) {
        if (someSpecificState) {
            return new RuntimeExceptionTypeA(ex);
        }
        return new RuntimeExceptionTypeB(ex);
    }
}
