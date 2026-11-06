package ghIssues;

public class Issue4427 {
    static int sink = 0;

    public static void copyInLoop(String[] args) {
        StringBuilder b0 = new StringBuilder();
        if (args.length == 0) {
            b0 = null;
        }
        for (int t = 0; t < 4; t++) {
            StringBuilder b1 = b0;
        }
        b0.append(sink); // NP_NULL_ON_SOME_PATH
        System.out.println(sink);
    }

    public static void noCopyInLoop(String[] args) {
        StringBuilder b0 = new StringBuilder();
        if (args.length == 0) {
            b0 = null;
        }
        for (int t = 0; t < 4; t++) {
            sink += t;
        }
        b0.append(sink); // NP_NULL_ON_SOME_PATH
        System.out.println(sink);
    }
}
