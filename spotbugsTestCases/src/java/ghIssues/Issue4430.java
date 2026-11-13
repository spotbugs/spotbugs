package ghIssues;

public class Issue4430 {
    private String compute() {
        return "value";
    }

    public boolean normalMethod() {
        String s = compute();
        return s instanceof String;
    }

    public Runnable lambdaMethod() {
        return () -> {
            String s = compute();
            if (s instanceof String) {
                System.out.println(s);
            }
        };
    }

    public Runnable anonymousMethod() {
        return new Runnable() {
            @Override
            public void run() {
                String s = compute();
                if (s instanceof String) {
                    System.out.println(s);
                }
            }
        };
    }
}
