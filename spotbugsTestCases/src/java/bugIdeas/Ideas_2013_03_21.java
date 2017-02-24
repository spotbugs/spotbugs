package bugIdeas;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2013_03_21 {
    
    @NoWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    boolean test(char u) {
        return -1 == (short) u;
    }

    @NoWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    boolean test(int u) {
        return -1 == (byte)(char)u;
    }
    
    @NoWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    boolean test2(char u) {
        return -1 == (byte)u;
    }
}
