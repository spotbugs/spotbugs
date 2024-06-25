package sfBugs;

import java.util.List;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug3126724 {
    @DesireNoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void main(String args[]) {
        for (int i = 1; i < args.length; i++) {
            if (i % 2 == 1) {
                System.out.print("");
            }
        }
    }

    @NoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void f(int i) {
        if (i >= 0 && i % 2 == 1)
            System.out.println("odd");
    }

    @NoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void f2(int i) {
        if (i > 0 && i % 2 == 1)
            System.out.println("odd");
    }

    @NoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void f3(int i) {
        if (Math.abs(i) % 2 == 1)
            System.out.println("odd");
    }

    @NoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void f4(int[] a) {
        if (a.length % 2 == 1)
            System.out.println("odd");
    }

    @NoWarning("IM_BAD_CHECK_FOR_ODD")
    public static void f5(List<Integer> lst) {
        if (lst.size() % 2 == 1)
            System.out.println("odd");
    }
}
