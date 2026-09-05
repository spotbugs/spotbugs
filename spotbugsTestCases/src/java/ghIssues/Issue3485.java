package ghIssues;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Issue3485 {
    public void foo() {
        if (nullReturn() > 300) {
            System.out.println("300");
        }
    }

    public void bar() {
        Long id = nullReturn();
        if (id < 100) {
            System.out.println("100");
        }
    }

    @CheckForNull
    public static Long nullReturn() {
        return null;
    }
}
