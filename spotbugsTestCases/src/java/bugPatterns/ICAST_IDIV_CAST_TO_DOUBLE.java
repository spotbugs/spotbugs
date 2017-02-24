package bugPatterns;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class ICAST_IDIV_CAST_TO_DOUBLE {

    @ExpectWarning("ICAST_IDIV_CAST_TO_DOUBLE")
    void bug(int x, int y) {
        double d = (x / y);
        System.out.println(d);
    }

}
