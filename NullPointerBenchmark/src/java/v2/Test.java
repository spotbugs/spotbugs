package v2;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Test {

    /**
     * Benchmark tests for null pointer defect detectors This benchmark tests
     * local variable tracking tracking tp1, tp2, tp3, tp4, tp5, tp6 : true
     * positive cases fp1, fp2, fp3, fp4 : false positives versions of the above
     * ifp1, ifp2, ifp3 : interprocedural false positives itp1 : true versions
     * of the ifp1
     */

    @NoWarning("NP")
    int fp1(int level) {
        Object x = null;
        if (level > 0)
            x = new Object();
        if (level > 4)
            return x.hashCode();
        return 0;
    }

    @DesireWarning("NP")
    int tp1(int level) {
        Object x = null;
        if (level > 0)
            x = new Object();
        if (level < 4)
            return x.hashCode();
        return 0;
    }

    @NoWarning("NP")
    int fp2(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        if (b)
            return x.hashCode();
        return 0;
    }

    @DesireWarning("NP")
    int tp2(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        if (!b)
            return x.hashCode();
        return 0;
    }

    @NoWarning("NP")
    int fp3(Object x) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }

    @ExpectWarning("NP")
    int tp3(Object x) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }

    @ExpectWarning("NP")
    int tp4(boolean b) {
        Object x = null;
        Object y = null;
        if (b)
            x = "x";
        if (x != null)
            y = "y";
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }

    @NoWarning("NP")
    int fp4(boolean b) {
        Object x = null;
        Object y = null;
        if (b)
            x = "x";
        if (x != null)
            y = "y";
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }
    @ExpectWarning("NP")
    int tp5(Object x) {
        if (x == null) {
            return x.hashCode();
        }
        return 0;
    }

    @ExpectWarning("NP")
    int tp6(Object x) {
        if (x == null) {
            Object y = x;
            return y.hashCode();
        }
        return 0;
    }

    int itp1(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        return helper1(x, b);
    }

    @NoWarning("NP")
    int ifp1(boolean b) {
        Object x = null;
        if (!b)
            x = new Object();
        return helper1(x, b);
    }

    @ExpectWarning("NP")
    int itp2() {
        return helper2(null);
    }

    @ExpectWarning("NP")
    int itp3(boolean b) {
        Object x = null;
        if (b)
            x = "x";
        return helper3(x);
    }
    @NoWarning("NP")
    int itf3(boolean b) {
        Object x = null;
        if (b)
            x = "x";
        if (!b)
            x = "y";
        return helper3(x);
    }

    // Bug when x is null and b is false
    private int helper1(Object x, boolean b) {
        if (b)
            return 0;
        return x.hashCode();
    }

    private int helper2(Object x) {
        return x.hashCode();
    }

    private int helper3(Object x) {
        return x.hashCode();
    }

}
