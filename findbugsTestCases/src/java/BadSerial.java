import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class BadSerial {
    @ExpectWarning("Se")
    static class NotFinal implements Serializable {
        static long serialVersionUID = 1;
    }

    @ExpectWarning("SS")
    static class NotStatic implements Serializable {
        final long serialVersionUID = 2;
    }

    @DesireWarning("Se")
    static class NotLong implements Serializable {
        static final int serialVersionUID = 3;
    }

    @NoWarning("Se")
    static class Good implements Serializable {
        static final long serialVersionUID = 4;
    }
}
