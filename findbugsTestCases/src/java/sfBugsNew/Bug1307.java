package sfBugsNew;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.detect.UnreadFields;

@DetectorUnderTest(UnreadFields.class)
public final class Bug1307 {

    @NoWarning("UWF_UNWRITTEN_FIELD")
    private static final String NOBUG = "nobug";

    @ExpectWarning("UWF_UNWRITTEN_FIELD")
    private static String BUG;

    @ExpectWarning("UWF_UNWRITTEN_FIELD")
    static String BUG1;

    public static boolean nobug(String s) {
        return "nobug".equals(s.isEmpty() ? NOBUG : s);
    }


    public static boolean bug(String s) {
        return "bug".equals(s.isEmpty() ? BUG : s);
    }

    public static boolean bug1(String s) {
        return "bug".equals(s.isEmpty() ? BUG1 : s);
    }

    public static boolean bug2(String s) {
        return "bug".equals(s.isEmpty() ? Constants1.BUG2 : s);
    }

    public static boolean bug3(String s) {
        return "bug".equals(s.isEmpty() ? Constants1.BUG3 : s);
    }

    public static boolean bug4(String s) {
        return "bug".equals(s.isEmpty() ? Constants2.A : s);
    }

    private Bug1307() {
    }
}

class Constants1 {
    @ExpectWarning("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    protected static String BUG2;

    @ExpectWarning("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    public static String BUG3;
}

class Constants2 {
    @ExpectWarning("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    public static String A;

    // no one reads
    @NoWarning("UWF_UNWRITTEN_FIELD")
    static String B2;

    // no one reads
    @NoWarning("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    public static String C2;

}

