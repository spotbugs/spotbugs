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
}
