package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1911620 {
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long getLongMinus1(String longStr) {
        long l = Long.valueOf(longStr);
        return --l;
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long getLongPlus1(String longStr) {
        long l = Long.valueOf(longStr);
        return ++l;
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long getLongMinus1Bad(String longStr) {
        long l = Long.valueOf(longStr);
        return l--;
    }

    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long getLongPlus1Bad(String longStr) {
        long l = Long.valueOf(longStr);
        return l++;
    }

    
    @ExpectWarning("DM_BOXED_PRIMITIVE_FOR_PARSING")
    public long getLongWithDLS(String longStr) {
        long l = Long.valueOf(longStr);
        long l2 = l; // This is the only place FindBugs should give a DLS
                     // warning
        return l;
    }

    public long getLongMinus1_2(String longStr) {
        long l = Long.parseLong(longStr);
        --l;
        return l;
    }

    public long getLongMinus2(String longStr) {
        long l = Long.parseLong(longStr);
        return l - 2;
    }

    public int getIntMinus1(String intStr) {
        int i = Integer.parseInt(intStr);
        return --i;
    }
    
    @ExpectWarning("DLS_DEAD_LOCAL_INCREMENT_IN_RETURN")
    public int getIntMinus1Bad(String intStr) {
        int i = Integer.parseInt(intStr);
        return i--;
    }
}
