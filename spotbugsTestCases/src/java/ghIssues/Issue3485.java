package ghIssues;

import org.jspecify.annotations.Nullable;

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

    public static @Nullable Long nullReturn() {
        return null;
    }
}
