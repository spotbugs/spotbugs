package sfBugsNew;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1347 {

    protected volatile List<Object> types;

    protected List<Object> types2;

    public static class B extends Bug1347 {
        @NoWarning("DC_DOUBLECHECK")
        List<Object> getTypes() {
            if (types == null) {
                synchronized (this) {
                    if (types == null) {
                        types = new ArrayList<>();
                    }
                }
            }
            return types;
        }

        @ExpectWarning("DC_DOUBLECHECK")
        List<Object> getTypes2() {
            if (types2 == null) {
                synchronized (this) {
                    if (types2 == null) {
                        types2 = new ArrayList<>();
                    }
                }
            }
            return types2;
        }
    }
}
