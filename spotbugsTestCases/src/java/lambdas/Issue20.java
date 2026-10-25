package lambdas;

import org.jspecify.annotations.Nullable;

public class Issue20 {

  /**
   * @return may be null
   */
  public static @Nullable String methodThatMightReturnNull() {
    if (Boolean.getBoolean("test")) {
      return null;
    }

    return "test";
  }

  public void testNPLambda() {
    Runnable lambda = () -> {
      String maybeNull = methodThatMightReturnNull();

      // would expect findbugs warning about possible null dereferencing here
      boolean empty = maybeNull.isEmpty();
      System.out.println(empty);
    };
    lambda.run();
  }

}
