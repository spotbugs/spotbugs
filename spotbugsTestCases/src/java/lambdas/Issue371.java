package lambdas;
import com.github.spotbugs.jsr305.annotation.CheckForNull;

public class Issue371 {

    @CheckForNull
    private String returnsNull() {
        return null;
    }

    public void dereferenceWithLambda() {
        returnsNull().chars().map(x -> 42);
    }
}
