package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

import org.jspecify.annotations.Nullable;

public class Bug3506402 {

    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    protected int caller(@Nullable Object param) {
        return param.hashCode();
    }

    @DesireWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    protected Object method(@Nullable Object... params) {
        return params.length;
    }

}
