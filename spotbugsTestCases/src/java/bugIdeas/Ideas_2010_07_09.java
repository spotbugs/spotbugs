package bugIdeas;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2010_07_09 {

    @NoWarning("BSHIFT")
    public static int test() {
        int num_bits = 32;
        int flip = (num_bits == 32) ? 0xffffffff : ((1 << num_bits) - 1);
        return flip;
    }
}
