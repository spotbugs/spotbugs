import edu.umd.cs.findbugs.annotations.CheckForNull;

public class Issue3481 {
    class Nested {
        void foo() {
            int n = nullReturn().length();
            System.out.println(n);
        }
    }

    @CheckForNull
    private String nullReturn() {
        return null;
    }
}
