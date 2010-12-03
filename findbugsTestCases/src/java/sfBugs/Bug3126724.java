package sfBugs;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug3126724 {
    @DesireNoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void main(String args[]) {
        for (int i = 1; i < args.length; i++) {
            if (i % 2 == 1) {
                System.out.print("");
            }
        }
    }
}
