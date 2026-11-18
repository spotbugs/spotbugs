package ghIssues;

public class Issue4428 {

    public static void ordinaryMethod() {
        ThreadGroup t = new ThreadGroup("ordinary");
    }

    public static void lambdaMethod() {
        Runnable r = () -> {
            ThreadGroup t = new ThreadGroup("lambda");
        };
        r.run();
    }

    public static void anonymousClassMethod() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ThreadGroup t = new ThreadGroup("anonymous");
            }
        };
        r.run();
    }
}
