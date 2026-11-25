package ghIssues.issue4426;

import org.jspecify.annotations.Nullable;

public class LoopUntilNonNull {
    static final class Node { int v; Node(int v) { this.v = v; } }
    static int sink = 0;

    @Nullable
    static Node lookup(String[] args, int i) {
        return i < args.length ? new Node(i) : null;
    }

    public static void main(String[] args) {
        Node n0 = null;
        int t = 0;
        while (n0 == null) {
            n0 = lookup(args, t++);  // the loop body assigns a result that may be null
        }
        sink += n0.v;                // safe: the loop only exits once n0 != null
        System.out.println(sink);
    }
}
