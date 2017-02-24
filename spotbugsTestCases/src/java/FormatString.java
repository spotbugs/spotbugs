import java.util.Date;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class FormatString {
    @ExpectWarning("FS")
    public static void main(String args[]) {
        falsePositive();

        java.util.Formatter f = new java.util.Formatter();
        System.out.println(f.format("%d %s %f %d %f", 8, "boy", 7.7, 9, 9.9, 10)); // warning
                                                                                   // !
        System.out.println(f.format("%d %s %f %d %f %d", 8, "boy", 7.7, 9, 9.9, 10, 11)); // warning
                                                                                          // !
        System.out.println(String.format("%d %n", 1, 2)); // warning!

        System.out.printf("%s\n", new int[] { 1, 2 });
        System.out.printf("%b\n", "false");
        System.out.printf("%d\n", "foo");
        System.out.printf("%d% \n", 42);
        System.out.printf("%d\n", new int[] { 1, 2 });
        System.out.printf("%s\n", (Object) new String[] { "a", "b" });

        varargsMethod((Object[]) args);
    }

    @NoWarning("FS")
    public static void falsePositive() {
        System.out.printf("last updated %1$tY-%1$tm-%1$te %1$tH:%1$tM:%1$tS.%1$tL, threshold %2$d min.%n", new Date(), 20);
        java.util.Formatter f = new java.util.Formatter();
        System.out.println(String.format("%d %s", 8, "boy")); // no warning
        System.out.println(f.format("%d %s %f %d %f %d", 8, "boy", 7.7, 9, 9.9, 10)); // no
                                                                                      // warning
        System.out.printf("%d", 7); // no warning
        System.out.format("%d", 8); // no warning
        System.out.println(f.format("%d %%%%%s %% %f %d %%%f %%%d", 8, "boy", 7.7, 9, 9.9, 10)); // no
                                                                                                 // warning
        System.out.println(String.format("%d %n", 1)); // no warning (%n does
                                                       // not consume an
                                                       // argument)

    }

    private static void varargsMethod(Object... args) {
        if (args[0] instanceof String) {
            System.out.println((String) args[0]);
        }
    }
}
