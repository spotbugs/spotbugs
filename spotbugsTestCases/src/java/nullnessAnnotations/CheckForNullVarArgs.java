package nullnessAnnotations;

import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
public class CheckForNullVarArgs {

    @NoWarning("NP,RCN")
    protected Object caller(@Nullable Object param) {
        final Object[] paramArray = param == null ? null : new Object[] { param };
        return method(paramArray);
    }

    @NoWarning("NP,RCN")
    protected Object method(@Nullable Object... params) {
        return params == null ? Boolean.FALSE : Boolean.TRUE;
    }

    @ExpectWarning("IL_INFINITE_RECURSIVE_LOOP")
    public void infiniteRecursiveLoop() {
        infiniteRecursiveLoop();
    }


}
