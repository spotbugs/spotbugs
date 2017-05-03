package sfBugsNew;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1219 {

    interface A {
    }

    interface B {
    }
    interface C {
    }
    interface D {
    }
    static class CC implements C {}
    static class DC implements D {}

    A objectA;

    @NoWarning(value="EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", confidence=Confidence.MEDIUM)
    @ExpectWarning(value="EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", confidence=Confidence.LOW)
    public boolean compare(B objectB) {
        return (objectA == objectB);
    }

    @NoWarning(value="EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", confidence=Confidence.MEDIUM)
    @ExpectWarning(value="EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", confidence=Confidence.LOW)
      public boolean compare(C objectC) {
        return (objectA == objectC);
    }

    @ExpectWarning(value="EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", confidence=Confidence.MEDIUM)
    public static boolean compare(C objectC, D objectD) {
      return (objectC == objectD);
  }
}
