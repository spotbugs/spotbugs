package bugIdeas;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Ideas_2012_01_04 {
    @DesireWarning("")
    static long getLong(byte [] b) {
       long result = 0;
        for(int i = 0; i < b.length; i++) {
            result = result << 8 + (b[i] & 0xff);
        }
        return result;
    }
}
