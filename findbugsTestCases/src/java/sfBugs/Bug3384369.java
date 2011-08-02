package sfBugs;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3384369 {

    @NoWarning("RCN")
    public Bug3384369(int[] value, int dataOffset) {
        assert (value == null || value.length > dataOffset) 
        : String.format(
                "Value array length %s should be greater than dataOffset %s", 
                (value == null ? 0 : value.length), dataOffset);
    }

}
