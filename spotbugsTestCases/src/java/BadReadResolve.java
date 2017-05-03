import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class BadReadResolve implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExpectWarning("Se")
    public BadReadResolve readResolve() {
        return new BadReadResolve();
    }

}
