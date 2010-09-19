package sfBugs;

public class Bug2436026 {

    private String[] args;

    private static String[] staticArgs;

    public Bug2436026(String... myArgs) {
        args = myArgs;
    }

    public static void setStatic(String... myArgs) {
        staticArgs = myArgs;
    }
}
