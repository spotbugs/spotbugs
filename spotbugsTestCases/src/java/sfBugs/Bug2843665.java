package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Bug2843665 {

    @DesireNoWarning("UPM")
    private static void setLogLevel(String logLevel) {
    }

    @DesireWarning("UPM")
    private static void neverCalled(String test) {
    }

    public static void main() {
        setLogLevel("test");
    }
}
