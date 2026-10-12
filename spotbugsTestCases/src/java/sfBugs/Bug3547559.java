package sfBugs;

import org.jspecify.annotations.NonNull;

import edu.umd.cs.findbugs.annotations.NoWarning;

@NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class Bug3547559 {


    private static final Bug3547559 instance = new Bug3547559();

    @NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    private static final @NonNull String VALUE = "test";

    public String doSomething() {
        if (instance != null) {
            return VALUE;
        }
        return "";
    }

}
