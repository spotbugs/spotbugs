package bugIdeas;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2015_02_02 {
    
//    public boolean isASCII(char c) [RuntimeInvisibleAnnotations]
//            Code(max_stack = 2, max_locals = 2, code_length = 14)
//            0:    iload_1
//            1:    iflt      #12
//            4:    iload_1
//            5:    bipush        80
//            7:    if_icmpgt     #12
//            10:   iconst_1
//            11:   ireturn
//            12:   iconst_0
//            13:   ireturn
            
    @DesireNoWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    public boolean isASCII(char c) {
        return c >= 0 && c < 80;
    }
    
    @ExpectWarning("INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE")
    public boolean isNonNegative(char c, char b) {
        return c >= 0 && b >= 0;
    }

}
