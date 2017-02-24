package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3412428 {
    
    float x0, xr, y0, yr, dn, tr, t0;
    boolean test;

    @NoWarning("FE_FLOATING_POINT_EQUALITY")
    public boolean doit() {
        float tot = 0;
        if (test) {
            tot += x0;
        }
        return tot <= 0 || tot != tot || Float.isInfinite(tot);
    }

    @DesireNoWarning("FE_FLOATING_POINT_EQUALITY")
    public boolean test2() {
        return x0*xr != x0*xr;
    }
    
    @NoWarning("FE_FLOATING_POINT_EQUALITY")
    public boolean test3() {
        float tmp = x0*xr;
        return tmp != tmp;
    }


    @NoWarning("FE_FLOATING_POINT_EQUALITY")
    protected float[] getColorValue(float x, float y, float[] out) {
        float v = (xr*(x-x0) + yr*(y-y0)) / dn;
        if (v >= 0 && v <= 1) {
            v = t0 + tr * v;
        } else if (v < 0) {
            v = test ? t0 : Float.NaN;
        } else {
            v = test ? t0 + tr : Float.NaN;
        }
        if (v == v) {
            out[0] = v;
            return out;
        } else {
            return null;
        }
    }


}
