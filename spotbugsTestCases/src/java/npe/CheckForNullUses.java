package npe;

import org.jspecify.annotations.Nullable;

public class CheckForNullUses {

    @Nullable
    Object doNotReport() {
        return this;
    }

    int bar(@Nullable Object x) {
        return x.hashCode();
    }

    int bar2() {
        return doNotReport().hashCode();
    }

}
