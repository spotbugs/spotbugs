package sfBugs;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3412428 {
    
    float floatField;
    double doubleField;
    
    @NoWarning("FE")
    public boolean isNaN(float b) {
        return b != b;
    }
    @NoWarning("FE")
    public boolean isNaN(double b) {
        return b != b;
    }
    @NoWarning("FE")
    public boolean hasNaN() {
        return floatField != floatField || doubleField != doubleField;
    }

}
