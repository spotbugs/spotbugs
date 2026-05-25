package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3918">#3918</a>.
 */
public class Issue3918 {
    @NoWarning("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public void rethrowFromCatch() {
        try {
            doSomething();
        } catch (RuntimeException ex) {
            ++errorCount;
            throw ex;
        }
    }

    @NoWarning("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public Issue3918() {
        try {
            doSomething();
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    @ExpectWarning("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    public void throwNewRuntimeException() {
        throw new RuntimeException();
    }

    private int errorCount;

    private void doSomething() {
        throw new RuntimeException();
    }
}
