package ghIssues.issue4426;

public class OverwrittenInLoop {
    static final class Node { int v; Node(int v) { this.v = v; } }
    static int sink = 0;

    public static void main(String[] args) {
        Node n0 = new Node(1);
        for (int t = 0; t < 1; t++) {
            n0 = null;               // overwritten before the back edge
            n0 = new Node(1);
        }
        sink += n0.v;                // safe: n0 is never null here
        System.out.println(sink);
    }
}
