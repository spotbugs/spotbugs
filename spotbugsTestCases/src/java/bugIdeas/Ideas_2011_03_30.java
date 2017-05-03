package bugIdeas;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2011_03_30 {

    @DesireWarning("BX_UNBOXING_IMMEDIATELY_REBOXED")
    public Long f(boolean b, Long x, long y) {
        Long z = b ? x : y;
        return z;
    }

    @ExpectWarning("BX_UNBOXING_IMMEDIATELY_REBOXED")
    public Long f2(boolean b, Long x, long y) {
        Long z = b ? y : x;
        return z;
    }
}
