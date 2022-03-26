import java.io.IOException;

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
}
