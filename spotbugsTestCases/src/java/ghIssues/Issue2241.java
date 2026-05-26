package ghIssues;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/2241">#2241</a>.
 */
public class Issue2241 {

    public static void descendingFloatLoop() {
        float x = 10.1f;
        while (x > 0) {
            System.out.println(x);
            x--;
        }
    }

    public static void incrementBeforeUse() {
        float x = 0.1f;
        while (x < 10) {
            x++;
            System.out.println(x);
        }
    }
}
