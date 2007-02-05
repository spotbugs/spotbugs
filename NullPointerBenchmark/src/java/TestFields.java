public class TestFields {
    /**
     * Benchmark tests for null pointer defect detectors
     * This benchmark tests field tracking
     * fp1, fp2, fp3 : false positives
     * tp1 : true positive
     * ifp1 : interprocedural false positives
     * itp1, itp2, itp3 : interprocedural true positives
     */

    Object x;

    TestFields(Object x) {
        this.x = x;
    }

    int fp1(int level) {
        x = null;
        if (level > 0)
            x = new Object();
        if (level > 4)
            return x.hashCode();
        return 0;
    }

    int fp2(boolean b) {
        x = null;
        if (b)
            x = new Object();
        if (b)
            return x.hashCode();
        return 0;
    }

    int fp3(boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }

    int tp1(boolean b) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }

    int ifp1(boolean b) {
        x = null;
        if (b)
            x = new Object();
        return helper1(b);
    }

    int itp1(boolean b) {
        x = null;
        if (!b)
            x = new Object();
        return helper1(b);
    }
    int itp2() {
        x = null;
        return helper2();
    }

    int itp3() {
        if (x == null)
            System.out.println("x is null");
        return helper2();
    }

    private int helper1(boolean b) {
        if (b)
            return 0;
        return x.hashCode();
    }

    private int helper2() {
        return x.hashCode();
    }

}
