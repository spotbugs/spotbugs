package ghIssues;

public class Issue3985 {

    public static int reproduceFN(boolean b, Object y) {
        if (b && y == null) {
            return 0;
        }
        return y.hashCode();
    }

    public static int reproduceFP(boolean b, Object y) {
        if (b && y != null) {
            return 1;
        }
        return y.hashCode();
    }
}
