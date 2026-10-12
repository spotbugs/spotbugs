package sfBugs;

import org.jspecify.annotations.NonNull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3061338 {

    @ExpectWarning("NP_NONNULL_PARAM_VIOLATION")
    public void doesntWork() {
        Object two = null;

        for (int i = 0; i < 5; i++) {
            // two is null and should be identified it as an invalid null input.
            getTwo(two);
        }
    }

    @ExpectWarning("NP_NONNULL_PARAM_VIOLATION")
    public void alreadyWorks() {
        Object two = null;

        for (int i = 0; i < 5; i++) {
            two = null;
            getTwo(two);
        }
    }

    public void getTwo(@NonNull Object x) {
    }

}
