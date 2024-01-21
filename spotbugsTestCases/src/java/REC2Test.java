import java.io.IOException;

/**
 * RECTest
 *
 * @author Adam Balogh
 */
public class REC2Test {
    public static Exception anException = new IOException();

    public static void staticThrowsException() throws Exception {
        throw new Exception();
    }

    public static void staticThrowsIOException() throws IOException {
        throw new IOException();
    }

    public void throwsNothing() {
    }

    public void throwsException() throws Exception {
        throw new Exception();
    }

    public void throwsIOException() throws IOException {
        throw new IOException();
    }

    public void throwsTwoExceptions() throws IOException, ClassNotFoundException {
        throw new IOException();
    }

    private void dontTriggerEmptyExceptionHandler() {
    }

    private class Resource implements AutoCloseable {
        @Override
        public void close() throws IOException {
        }
    }

    private Resource getResource() throws IOException {
        return new Resource();
    }

    private interface Unknown {
        void doesSomething();
    }

    // should fail -- catches both Exception and RuntimeException
    public void testFail() {
        try {
            for (int i = 0; i < 1000; i++)
                for (int j = i; j < 1000; j++)
                    throwsNothing();
            throwsIOException();
        } catch (RuntimeException e) {
            dontTriggerEmptyExceptionHandler();
        } catch (Exception e) {
            dontTriggerEmptyExceptionHandler();
        }
    }

    // should fail -- catches RuntimeException
    public void testFail2() {
        try {
            for (int i = 0; i < 1000; i++)
                for (int j = i; j < 1000; j++)
                    throwsNothing();
        } catch (RuntimeException e) {
            dontTriggerEmptyExceptionHandler();
        }
    }

    // should fail -- catches Throwable
    public void testFail3() {
        try {
            for (int i = 0; i < 1000; i++)
                for (int j = i; j < 1000; j++)
                    throwsNothing();
            throwsIOException();
        } catch (Throwable t) {
            dontTriggerEmptyExceptionHandler();
        }
    }

    // should pass -- rethrows both Exception and RuntimeException
    public void testPass() {
        try {
            for (int i = 0; i < 1000; i++)
                for (int j = i; j < 1000; j++)
                    throwsNothing();
            throwsIOException();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // should pass -- rethrows Throwable
    public void testPass2() {
        try {
            for (int i = 0; i < 1000; i++)
                for (int j = i; j < 1000; j++)
                    throwsNothing();
            throwsIOException();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // should pass -- The try-with-resources construct catches Throwable but it
    // is skipped in the detector.
    public void testPass3() throws IOException {
        try (Resource res = getResource()) {
        }
    }

    // should pass -- Invokation of an unknown method may require catching of
    // `RutimeException`, `Exception` or `Throwable`.
    public <U extends Unknown> void testPass4(U u) {
        try {
            u.doesSomething();
        } catch (RuntimeException e) {
            dontTriggerEmptyExceptionHandler();
        }
    }

    public class TestPass5<U extends Unknown> {
        // should pass -- Invokation of an unknown method may require catching of
        // `RutimeException`, `Exception` or `Throwable`.
        public void testPass5(U u) {
            try {
                u.doesSomething();
            } catch (RuntimeException e) {
                dontTriggerEmptyExceptionHandler();
            }
        }
    }
}
