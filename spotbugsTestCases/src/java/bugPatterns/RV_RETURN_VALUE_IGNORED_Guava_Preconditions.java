package bugPatterns;

import com.google.common.base.Preconditions;

public class RV_RETURN_VALUE_IGNORED_Guava_Preconditions {
    public int method(String param) {
        Preconditions.checkNotNull(param);
        return param.length();
    }
}
