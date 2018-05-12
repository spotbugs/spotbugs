package bugPatterns;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;

public class NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE_Guava_Preconditions {
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public int method(@Nullable String param) {
        Preconditions.checkNotNull(param);
        return param.length();
    }
}
