package ghIssues;

public class Issue4430 {
    public void run() {
        Runnable r = () -> {
            String s = compute();
            if (s instanceof String) {
                System.out.println(s);
            }
        };
        r.run();
    }

    private String compute() {
        return "a";
    }
}
