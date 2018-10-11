package nullnessAnnotations;

import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Reproducer for https://github.com/spotbugs/spotbugs/issues/772
 */
public class NonNullParameterOfInnerClassConstructor {

  private class Inner {
    private final String a;
    private final Object b;

    @NoWarning("NP")
    Inner(@NonNull String a, @Nullable Object b) {
      this.a = a.toLowerCase();
      this.b = b;
    }

  }
}
