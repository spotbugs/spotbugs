import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

class BadSerial {
    static class NotFinal implements Serializable {
        @ExpectWarning("Se")
        static long serialVersionUID = 1;
    }

    static class NotStatic implements Serializable {
        @ExpectWarning("SS")
        final long serialVersionUID = 2;
    }

    static class NotLong implements Serializable {
        @DesireWarning("Se")
        static final int serialVersionUID = 3;
    }

    static class Good implements Serializable {
        @NoWarning("Se")
        static final long serialVersionUID = 4;
    }
}
