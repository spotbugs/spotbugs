package bugIdeas;

import java.util.Random;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2012_11_15 {
    
    Random r = new Random();
    public @CheckForNull Object get() {
        if (r.nextBoolean())
            return "x";
        return null;
    }
    
    @ExpectWarning("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @NoWarning("NP_GUARANTEED_DEREF")
    public int f(int i) {
        
        Object x = get();
        
        if (i > 0)
            return x.hashCode();
        else
            return  -x.hashCode();
    }

}
