public class Test {
    
    /**
     * Benchmark tests for null pointer defect detectors
     * This benchmark tests local variable tracking tracking
     * fp1, fp2, fp3 : false positives
     * tp1 : true positive
     * ifp1 : interprocedural false positives
     * itp1, itp2, itp3 : interprocedural true positives
     */

    int fp1(int level) {
        Object x = null;
        if (level > 0)
            x = new Object();
        if (level > 4)
            return x.hashCode();
        return 0;
    }

    int fp2(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        if (b)
            return x.hashCode();
        return 0;
    }

    int fp3(Object x, boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }

    int tp1(Object x, boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }

    int ifp1(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        return helper1(x, b);
    }

    int itp1(boolean b) {
        Object x = null;
        if (!b)
            x = new Object();
        return helper1(x, b);
    }
    int itp2() {
        return helper2(null);
    }

    int itp3(Object x) {
        if (x == null)
            System.out.println("x is null");
        return helper2(x);
    }

    private int helper1(Object x, boolean b) {
        if (b)
            return 0;
        return x.hashCode();
    }

    private int helper2(Object x) {
        return x.hashCode();
    }

}
