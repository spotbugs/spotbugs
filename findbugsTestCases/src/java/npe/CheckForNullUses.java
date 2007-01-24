package npe;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class CheckForNullUses {
    
    @CheckForNull Object doNotReport() {
        return this;
    }
    
    int bar(@CheckForNull Object x) {
        return x.hashCode();
    }
    int bar2() {
        return doNotReport().hashCode();
    }

}
