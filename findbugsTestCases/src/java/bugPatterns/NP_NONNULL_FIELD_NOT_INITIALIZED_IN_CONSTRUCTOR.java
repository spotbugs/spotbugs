package bugPatterns;

import javax.annotation.Nonnull;

public class NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR {
    
    
    @Nonnull static Object a,b;
    
    @Nonnull Object x,y;
    
    static {
        a = "a";
    }
    
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR() {
        x = y = "a";
    }
    
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR(String a) {
        x = a;
    }
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR(int z) {
        this();
    }
    NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR(double z) {
        super();
    }
}
