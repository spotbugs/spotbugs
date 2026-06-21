package ghIssues;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Issue3808 {

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public void suppressedMethod() {
        Runnable r = () -> {
            Integer.valueOf(1);
        };
        r.run();
    }

    public void notSuppressedMethod() {
        Runnable r = () -> {
            Integer.valueOf(1);
        };
        r.run();
    }
}
