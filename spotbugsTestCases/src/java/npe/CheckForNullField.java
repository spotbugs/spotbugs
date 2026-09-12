package npe;

import org.jspecify.annotations.Nullable;

public class CheckForNullField {
    @Nullable Object x;

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
