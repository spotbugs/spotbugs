package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2011_08_02 {
    
    @ExpectWarning("INT_BAD_COMPARISON_WITH_INT_VALUE")
    public boolean badCheck(int x) {
        return x == 9999999999L;
    }
    @ExpectWarning("INT_BAD_COMPARISON_WITH_INT_VALUE")
    public boolean badCheck2(int x) {
        return 9999999999L == x ;
    }
    @NoWarning("INT_BAD_COMPARISON_WITH_INT_VALUE")
    public boolean okCheck(int x) {
        return x == 999999999L;
    }

}
