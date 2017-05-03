package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3097362 {

    int x;
    
    public Bug3097362(int x) {
        this.x = x;
    }
    
    public int hashCode() {
        return x;
        
    }
    
   @NoWarning("BC_EQUALS_METHOD_SHOULD_WORK_FOR_ALL_OBJECTS")
    public boolean equals(Object obj) {
        if (!this.getClass().isInstance(obj)) {
            return false;
        }

        Bug3097362 mo = (Bug3097362) obj; /*
                                           * <- find bugs error - Equals method
                                           * should not assume anything...
                                           */
        return this.x == mo.x;
    }

}
