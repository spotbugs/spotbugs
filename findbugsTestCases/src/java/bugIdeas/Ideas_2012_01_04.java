package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2012_01_04 {
    @ExpectWarning("")
    static long getLong(byte [] b) {
       long result = 0;
        for(int i = 0; i < b.length; i++) {
            result = result << 8 + (b[i] & 0xff); // parsed as result << (8 + (b[i] & 0xff));
        }
        return result;
    }
    
    @NoWarning("")
    static long getLong2(byte [] b) {
       long result = 0;
        for(int i = 0; i < b.length; i++) {
            result = result << 8 | (b[i] & 0xff); // parsed as result << (8 + (b[i] & 0xff));
        }
        return result;
    }
    
    @NoWarning("")
    static long getLongFixed(byte [] b) {
       long result = 0;
        for(int i = 0; i < b.length; i++) {
            result = (result << 8) + (b[i] & 0xff); 
        }
        return result;
    }
}
