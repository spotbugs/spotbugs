package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3135098 {
    @DesireNoWarning("SF_SWITCH_NO_DEFAULT")
    public int go(MyEnum e) {
        switch (e) {
            case A: return 1;
            case B: return 2;
        }
        return 3;
    }
    private enum MyEnum { A, B}
}

