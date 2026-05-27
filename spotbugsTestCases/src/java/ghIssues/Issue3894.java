package ghIssues;

public class Issue3894 {

    static int x = 0;
    static int y = 0;

    static void trigger() throws Exception {
        throw new Exception();
    }

    public static void multipleCasesInMethod() {
        if (x > 0) {
        }

        try {
            trigger();
        } catch (Exception e) {
            if (x == 0) {
            }
        }

        if (y != 0) {
        }

        if (x == 0 && true) {
        }
    }

    public static void multipleEmptyIfs(int a, int b, int c) {
        if (a > 0) {
        }
        if (b > 0) {
        }
        if (c > 0) {
        }
    }

    public static void nonEmptyIf() {
        if (x > 0) {
            y = 1;
        }
    }
}
