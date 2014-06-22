package sfBugs;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class RFE1910461 {

    @ExpectWarning("DLS_DEAD_LOCAL_STORE,DLS_DEAD_LOCAL_STORE_IN_RETURN")
    boolean high(boolean b1, boolean b2, boolean b3) {
        if (b1)
            return b1 = b2;
        else
            return b2 = b3;
    }

    @ExpectWarning("DLS_DEAD_LOCAL_STORE,DLS_DEAD_LOCAL_STORE_IN_RETURN")
    int medium(int x) {
        int m = 10;
        if (x <= 0)
            return m = 8;
        return m * x;
    }

    @ExpectWarning("DLS_DEAD_LOCAL_STORE,DLS_DEAD_LOCAL_STORE_IN_RETURN")
    int low(int x) {
        int m = 10;
        if (x > 0)
            return m * x;
        return m *= x;
    }

    @ExpectWarning("DLS_DEAD_LOCAL_STORE_IN_RETURN")
    String lowString(int x) {
        String s = "foo";
        if (x < 0)
            return s += x;
        return s;
    }

    String fpString(int x) {
        String s = "foo";
        if (x >= 0)
            return s;
        return s += x;
    }

}
