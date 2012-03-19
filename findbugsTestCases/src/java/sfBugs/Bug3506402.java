package sfBugs;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug3506402 {

    @ExpectWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    protected int caller(@CheckForNull Object param) {
        return param.hashCode();
    }
    
    @DesireWarning("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    protected Object method(@CheckForNull Object... params) {
        return params.length;
    }

}
