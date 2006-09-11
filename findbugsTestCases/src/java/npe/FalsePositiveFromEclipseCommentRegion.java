package npe;

// false positive first found in
// org/eclipse/pde/internal/CommentRegion.formatRegion(final String indentation, final int width) {

public class FalsePositiveFromEclipseCommentRegion {
	Object foo() {
		return new Object();
	}

	int f(int count) {

		Object p;
		Object n = null;

		for (int i = 0; i < count; i++) {
			p = n;
			n = foo();
		}
		return  n.hashCode();
	}

}
