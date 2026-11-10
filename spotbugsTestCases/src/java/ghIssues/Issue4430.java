package ghIssues;
public class Issue4430 {

    // Normal method: warning expected.
    public static void normalMethod(String value) {
        if (value instanceof String) {
            System.out.println(value);
        }
    }

    // Lambda body: warning expected.
    public static void lambdaMethod(String value) {
        Runnable runnable = () -> {
            if (value instanceof String) {
                System.out.println(value);
            }
        };
        runnable.run();
    }

    // Anonymous class: warning expected.
    public static void anonymousMethod(String value) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (value instanceof String) {
                    System.out.println(value);
                }
            }
        };
        runnable.run();
    }
}
