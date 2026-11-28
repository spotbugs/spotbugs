package ghIssues.issue4426.variant;

public class Exit {
    static final class Node { int v; Node(int v) { this.v = v; } }
    static int sink = 0;

    public static void main(String[] args) {
        Node n0 = new Node(1);
        for (int t = 0; t < 1; t++) {
            n0 = null;               // the same assignment inside a loop body
        }
        sink += n0.v;                // NullPointerException at run time; nothing reported
        System.out.println(sink);
    }
}
