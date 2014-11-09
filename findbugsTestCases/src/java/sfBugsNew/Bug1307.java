package sfBugsNew;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.UnreadFields;

@DetectorUnderTest(UnreadFields.class)
public final class Bug1307 {

    // Findbugs has an issue with javac Java 8 compiled code here.
    // Compiling the code with ecj target 8 or javac 8 but target 7 is OK.
    // @NoWarning("UWF_UNWRITTEN_FIELD")
    @DesireNoWarning("UWF_UNWRITTEN_FIELD")
    private static final String NOBUG = "nobug";

    public static boolean bug(String s) {
        return "nobug".equals(s.isEmpty() ? NOBUG : s);
    }

    @ExpectWarning("UWF_UNWRITTEN_FIELD")
    private static String BUG;

    public static boolean bug2(String s) {
        return "bug".equals(s.isEmpty() ? BUG : s);
    }

    private Bug1307() {
    }
}