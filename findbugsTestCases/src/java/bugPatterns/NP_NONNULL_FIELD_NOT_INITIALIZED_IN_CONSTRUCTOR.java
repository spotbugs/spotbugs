package bugPatterns;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR {
    
    
    @Nonnull static Object a;
    
    @ExpectWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    @Nonnull static Object b;
    
    @ExpectWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    @Nonnull Object x;
    
    @ExpectWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    @Nonnull Object y;
    
    static {
        a = "a";
    }
    
    @NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR() {
        x = y = "a";
    }
    
    @ExpectWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR(String a) {
        x = a;
    }
    
    @NoWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR(int z) {
        this();
    }
    
    @ExpectWarning("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR(double z) {
        super();
    }
}
