package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1361 {
    @NoWarning("UC")
    public void test(boolean flag) {
        if (!flag) {
            System.out.println("No flag");
        } else {
            System.out.println("!flag = " + (!flag));
        }
    }

    @ExpectWarning("UC_USELESS_CONDITION")
    public int testInt(boolean flag) {
        if(!flag) {
            return 5 + (flag ? 0 : 1);
        }
        return 0;
    }

    @ExpectWarning("UC_USELESS_CONDITION")
    public int testInt2(boolean flag) {
        if(flag) {
            return 5 - (flag ? 1 : 0);
        }
        return 0;
    }

    @ExpectWarning("UC_USELESS_CONDITION")
    public int testInt3(boolean flag) {
        if(flag) {
            return (flag ? 1 : 0) * (Math.round(10.0f * 5) * 10);
        }
        return 0;
    }

    @ExpectWarning("UC_USELESS_CONDITION")
    public int testInt4(boolean flag, boolean flag2, boolean flag3) {
        if(flag) {
            return (flag ? 1 : 0) + (flag2 && flag3 ? 1 : flag3 ? 2 : 0);
        }
        return 0;
    }
}
