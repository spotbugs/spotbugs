package ghIssues.issue4426.seed;

public class Exit {
    static final class Node { int v; Node(int v) { this.v = v; } }
    static int sink = 0;

    public static void main(String[] args) {
        Node n0 = new Node(1);
        if (args.length == 0) {
            n0 = null;               // the assignment under a branch
        }
        sink += n0.v;                // NP_NULL_ON_SOME_PATH reported
        System.out.println(sink);
    }
}
