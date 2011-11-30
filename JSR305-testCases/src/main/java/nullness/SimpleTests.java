package nullness;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class SimpleTests {

    int f(@CheckForNull Object x) {
        return x.hashCode();
    }
    @Nonnull Object g() {
        return null;
    }

}
