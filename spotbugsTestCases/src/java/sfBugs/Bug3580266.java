package sfBugs;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3580266 {
    
    @NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    @Nonnull
    private int scalar;
    
    @NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    Bug3580266() {}
}
