import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.Confidence;

class AnnotationTest {
    @CheckReturnValue(confidence = Confidence.HIGH)
    int f() {
        return 42;
    }
}
