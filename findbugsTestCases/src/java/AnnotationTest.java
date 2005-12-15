import edu.umd.cs.findbugs.annotations.*;

class AnnotationTest {
	@CheckReturnValue(priority = Priority.HIGH)
	int f() {
		return 42;
	}
}
