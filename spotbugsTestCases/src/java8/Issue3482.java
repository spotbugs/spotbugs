import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Issue3482 {
    public void foo(String[] args) {
        new Runnable() {
            @Override
            public void run() {
                int n = nullReturn().length();
                System.out.println(n);
            }
        };
    }

    @CheckForNull
    private String nullReturn() {
        return null;
    }
}
