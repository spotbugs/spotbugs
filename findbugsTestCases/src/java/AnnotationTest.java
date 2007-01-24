import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.Priority;

class AnnotationTest {
	@CheckReturnValue(priority = Priority.HIGH)
	int f() {
		return 42;
	}
}
