package ghIssues;

/**
 * Issue 4137: {@code AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE} was missed
 * when the non-atomic read-modify-write of a shared field lives in a
 * non-static inner class that mutates a field of its enclosing (multi-threaded)
 * class via the synthetic {@code this$0} reference.
 */
public class Issue4137 extends Thread {
    private int num = 0;

    public void toggle() {
        num = num + 2;
    }

    public class SubAdditionOnSharedVariable {
        public void toggle2() {
            num = num + 2;
        }
    }

    /**
     * A static nested class has no enclosing instance, so it must not inherit
     * the enclosing Thread's multi-threaded context. Its own non-atomic
     * read-modify-write is therefore not analyzed here, even though it has the
     * same shape (own field, read in another method) as a genuine shared bug.
     */
    public static class StaticNested {
        private int own = 0;

        public void toggle3() {
            own = own + 2;
        }

        public int getOwn() {
            return own;
        }
    }
}
