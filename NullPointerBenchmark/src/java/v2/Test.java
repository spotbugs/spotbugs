package v2;

public class Test {
    
    /**
     * Benchmark tests for null pointer defect detectors
     * This benchmark tests local variable tracking tracking
     * fp1, fp2, fp3, fp4, fp5 : false positives
     * tp1, tp2, tp3, tp4, tp5 : true positive versions of the above
     * ifp1, ifp2, ifp3 : interprocedural false positives
     * itp1 : true versions of the ifp1
     */

    int fp1(int level) {
        Object x = null;
        if (level > 0)
            x = new Object();
        if (level > 4)
            return x.hashCode();
        return 0;
    }

    int tp1(int level) {
        Object x = null;
        if (level > 0)
            x = new Object();
        if (level < 4)
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
    int tp2(boolean b) {
        Object x = null;
        if (b)
            x = new Object();
        if (!b)
            return x.hashCode();
        return 0;
    }

    int fp3(Object x) {
        Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }
    int tp3(Object x) {
       	Object y = null;
        if (x != null)
            y = new Object();
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }
    int tp4(boolean b) {
    	Object x = null;
        Object y = null;
        if (b) x = "x";
        if (x != null) y = "y";
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return x.hashCode();
    }
    int fp4(boolean b) {
       	Object x = null;
        Object y = null;
        if (b) x = "x";
        if (x != null) y = "y";
        if (y != null)
            return x.hashCode() + y.hashCode();
        else
            return 0;
    }
    int tp5(Object x) {
    	if (x == null) {
    		return x.hashCode();
    	}
    	return 0;
    }
   
    int tp6(Object x) {
    	if (x == null) {
    		Object y = x;
    		return y.hashCode();
    	}
    	return 0;
    }
  
    int itp1(boolean b) {
        Object x = null;
        if (b) x = new Object();
        return helper1(x, b);
    }

    int ifp1(boolean b) {
        Object x = null;
        if (!b) x = new Object();
        return helper1(x, b); 
    }
    int itp2() {
        return helper2(null);
    }

    int itp3(boolean b) {
    	Object x = null;
        if (b) x = "x";   
        return helper3(x);
    }
    
    

    // Bug when x is null and b is false
    private int helper1(Object x, boolean b) {
        if (b) return 0;
        return x.hashCode();
    }

    private int helper2(Object x) {
        return x.hashCode();
    }
    private int helper3(Object x) {
        return x.hashCode();
    }

}
