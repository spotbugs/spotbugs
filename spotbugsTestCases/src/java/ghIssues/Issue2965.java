package ghIssues;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Issue2965 {

  public void checkForNullRequireNonNull() {
    final String foo = getString("foo");
    // Getting an NPE here is the point of requireNonNull(), we should not report NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
    Objects.requireNonNull(foo);
  }

  public void checkForNullRequireNonNullWithMessage() {
    final String bar = getString("bar");
    // Getting an NPE here is the point of requireNonNull(), we should not report NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
    Objects.requireNonNull(bar, "Bar must not be null");
  }

  public void checkForNullRequireNonNullWithMessageMultiLine() {
    final String bar = getString("bar");
    // Getting an NPE here is the point of requireNonNull(), we should not report NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
    Objects.requireNonNull(
    		bar, 
    		"Bar must not be null");
  }

  public void checkForNullRequireNonNullWithMessageSupplier() {
    final Supplier<String> supplier = ()-> "Baz must not be null";
    final String baz = getString("baz");
    // Getting an NPE here is the point of requireNonNull(), we should not report NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
    Objects.requireNonNull(baz, supplier);
  }

  public void conditionalCheckForNull(boolean flag) {
    String foo = getString("foo");
    if (flag)  {
      foo = "x";
    }

    Objects.requireNonNull(
      foo,
            () -> "missing value"
    );
  }

  @CheckForNull
  private String getString(@Nullable final String text) {
    return text == null ? null : text;
  }

  public void nullRequireNonNull() {
    final String foo = null;
    Objects.requireNonNull(foo); // NP_LOAD_OF_KNOWN_NULL_VALUE
  }
  
  public String requireNonNullDereference() {
    final String foo = getString("foo");
    return foo.trim(); // NP_LOAD_OF_KNOWN_NULL_VALUE
  }
}