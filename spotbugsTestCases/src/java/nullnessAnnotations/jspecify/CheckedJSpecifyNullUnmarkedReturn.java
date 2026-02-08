package nullnessAnnotations.jspecify;
import org.jspecify.annotations.NullUnmarked;


public class CheckedJSpecifyNullUnmarkedReturn {
    @NullUnmarked
    String foo() {
        return null;
    }

    void bar() {
        String foo = foo();
        if (foo != null) {
            System.out.println(foo.hashCode());
        }
    }
}
