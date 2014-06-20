package nullnessAnnotations;


import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

@ParametersAreNonnullByDefault
public class CheckForNullArrayArgs {

    @NoWarning("NP,RCN")
    protected Object caller(@CheckForNull Object param) {
        final Object[] paramArray = param == null ? null : new Object[] { param };
        return methodTakingArray(paramArray);
    }

    @NoWarning("NP,RCN")
    protected Object methodTakingArray(@CheckForNull Object[] params) {
        return params == null ? Boolean.FALSE : Boolean.TRUE;
    }

    @ExpectWarning("IL_INFINITE_RECURSIVE_LOOP")
    public void infiniteRecursiveLoop() {
            infiniteRecursiveLoop();
    }

}
