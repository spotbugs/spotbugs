package npe;

import org.jspecify.annotations.Nullable;

public class InsideCatchBlock {
    public int doNotReportCatchNullPointerException(@Nullable Object x) {
        try {
            return x.hashCode();
        } catch (NullPointerException e) {
            return 42;
        }
    }

    public int doNotReportCatchRuntimeException(@Nullable Object x) {
        try {
            return x.hashCode();
        } catch (RuntimeException e) {
            return 42;
        }
    }

    public int doNotReportCatchException(@Nullable Object x) {
        try {
            return x.hashCode();
        } catch (Exception e) {
            return 42;
        }
    }

}
