package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3552708 {
    
    @NoWarning("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    public IllegalAccessError knownSelfType() {
        return (IllegalAccessError) new IllegalAccessError().initCause(new IllegalAccessException());
    }

}
