package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1408 {
    private static final int flag1 = 0x40000000;
    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug1(int value) {
        boolean flagSet = ((value & flag1) > 0);
        return flagSet;
    }

    private final int flag2 = 0x40000000;
    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug2(int value) {
        boolean flagSet = ((value & flag2) > 0);
        return flagSet;
    }

    private static final int flag3 = 0x40000000;
    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug3(int value) {
        boolean flagSet = ((flag3 & value) > 0);
        return flagSet;
    }

    private final int flag4 = 0x40000000;
    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug4(int value) {
        boolean flagSet = ((flag4 & value) > 0);
        return flagSet;
    }

    private static int flag5 = 0x40000000;
    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug5(int value) {
        boolean flagSet = ((value & flag5) > 0);
        return flagSet;
    }

    private int flag6 = 0x40000000;
    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug6(int value) {
        boolean flagSet = ((value & flag6) > 0);
        return flagSet;
    }

    @ExpectWarning("BIT_SIGNED_CHECK")
    public boolean bug7(int value) {
        int flag7 = 0x40000000;
        boolean flagSet = ((value & flag7) > 0);
        return flagSet;
    }
}
