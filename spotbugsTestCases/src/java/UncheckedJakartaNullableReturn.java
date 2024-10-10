import jakarta.annotation.Nullable;

public class UncheckedJakartaNullableReturn {
    @Nullable
    String foo() {
        return null;
    }

    void bar() {
        System.out.println(foo().hashCode());
    }
}
