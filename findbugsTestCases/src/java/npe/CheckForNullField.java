package npe;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class CheckForNullField {
    @CheckForNull
    Object x;

    public int getNonNullXDoNotReport() {
        if (x == null)
            x = new Object();
        return x.hashCode();
    }

    public Object getNonNullXDoNotReport2() {
        if (x != null)
            return x.hashCode();
        x = new Object();
        return x.hashCode();
    }
}
