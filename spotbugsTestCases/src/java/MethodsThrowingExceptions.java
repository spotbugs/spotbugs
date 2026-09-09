import java.io.IOException;
import java.util.concurrent.Callable;

@SuppressWarnings({"RedundantThrows", "unused", "Convert2Lambda"})
public class MethodsThrowingExceptions {
    boolean isCapitalizedThrowingRuntimeException(String s) {
        if (s == null) {
          throw new RuntimeException("Null String");
        }
        if (s.equals("")) {
          return true;
        }
        String first = s.substring(0, 1);
        String rest = s.substring(1);
        return first.equals(first.toUpperCase()) && rest.equals(rest.toLowerCase());
    }

    boolean isCapitalizedThrowingSpecializedException(String s) {
        if (s == null) {
          throw new NullPointerException();
        }
        if (s.equals("")) {
          return true;
        }
        String first = s.substring(0, 1);
        String rest = s.substring(1);
        return first.equals(first.toUpperCase()) && rest.equals(rest.toLowerCase());
    }

    private void methodThrowingBasicException() throws Exception {
        System.out.println("Method throwing Exception");
    }
    
    private void methodThrowingIOException() throws IOException {
        System.out.println("Method throwing IOException");
    }
    
    private void methodThrowingThrowable() throws Throwable {
        System.out.println("Method throwing Throwable");
    }

    private void methodThatUsesAnonymousThatImplementsMethodThatThrowsException() {
        final Callable<String> callable = new Callable<String>() {
            public String call() {
                return "test";
            }
        };

        acceptCallable(callable);
    }

    private void methodThatUsesSyntheticThatThrowsException() {
        acceptCallable(() -> "test2");
    }

    private void methodThatUsesAnonymousThatImplementsMethodThatThrowsThrowable() {
        final ThrowThrowable runnable = new ThrowThrowable() {
            public void run() {
            }
        };

        acceptThrowingRunnable(runnable);
    }

    private void methodThatUsesSyntheticThatThrowsThrowable() {
        acceptThrowingRunnable(() -> {});
    }

    private void acceptCallable(Callable<String> callable) {
        try {
            System.out.println(callable.call());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void acceptThrowingRunnable(ThrowThrowable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            System.err.println(e.getMessage());
        }
    }

    @FunctionalInterface
    interface ThrowThrowable {
        void run() throws Throwable;
    }
}
