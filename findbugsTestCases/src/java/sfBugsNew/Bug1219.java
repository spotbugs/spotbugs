package sfBugsNew;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1219 {

    interface A {
    }

    interface B {
    }

    A objectA;

    @NoWarning(value="EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", confidence=Confidence.MEDIUM)
    public boolean compare(B objectB) {
        return (objectA == objectB);
    }
}
