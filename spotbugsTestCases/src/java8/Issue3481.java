import org.jspecify.annotations.Nullable;

public class Issue3481 {
    class Nested {
        void foo() {
            int n = nullReturn().length();
            System.out.println(n);
        }
    }

    private @Nullable String nullReturn() {
        return null;
    }
}
