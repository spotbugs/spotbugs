package npe;

// false positive first found in
// org/eclipse/pde/internal/CommentRegion.formatRegion(final String indentation, final int width) {

public class FalsePositiveFromEclipseCommentRegion {
    static Object foo() {
        return new Object();
    }

    static int f() {

        Object p;
        Object n = null;

        for (int i = 5; i > 0; i--) {
            p = n;
            n = foo();
        }
        return n.hashCode();
    }

}
