package nullnessAnnotations;

import jakarta.annotation.Nonnull;

public class CheckedJakartaNonnullReturn {
    @Nonnull
    String foo() {
        return "non-null string"; // This is covered by @Nonnull contract, so no warning should be raised
    }

    void bar() {
        System.out.println(foo().hashCode()); // This should not trigger any warning from SpotBugs
    }
}
