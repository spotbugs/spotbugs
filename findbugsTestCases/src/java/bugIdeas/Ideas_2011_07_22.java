package bugIdeas;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_07_22 {
    
    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode(Object x) {
        Preconditions.checkArgument(x != null, "x is null");
        return x.hashCode();
    }
    
    @NoWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode2(Object x) {
        Preconditions.checkNotNull(x, "x is null");
        return x.hashCode();
    }

    @DesireNoWarning("NP_NULL_ON_SOME_PATH")
    public int getHashCode3(Object x) {
        Preconditions.checkNotNull(x, "x is null");
        if (x == null)
            System.out.println("huh?");
        return x.hashCode();
    }

    
}
