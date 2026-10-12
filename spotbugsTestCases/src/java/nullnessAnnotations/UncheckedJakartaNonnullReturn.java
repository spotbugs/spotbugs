package nullnessAnnotations;

import org.jspecify.annotations.NonNull;

public class UncheckedJakartaNonnullReturn {
    @NonNull
    String foo() {
        return null; // This should trigger the SpotBugs rule since @Nonnull is violated
    }
    
    void bar() {
        System.out.println(foo().hashCode()); // This line might trigger a warning from SpotBugs
    }
}
