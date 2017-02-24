package sfBugs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1874856 {

    public static void main(String args[]) {
        falsePositive();
    }

    @ExpectWarning("FS")
    public static void falsePositive() {
        // None of these should yield warnings; NOT TRUE
        Calendar c = new GregorianCalendar(1993, 4, 23);
        String s1 = String.format("s1 Duke's Birthday: %1$tm %1$te, %1$tY", c);
        System.out.println(s1);
        String s2 = String.format("s2 Duke's Birthday: %1$tm %<te, %<tY", c);
        System.out.println(s2);
        String s3 = String.format("s3 Duke's Birthday: %2$tm %<te, %<tY", c, c);
        System.out.println(s3);
        String s4 = String.format("s4 Duke's Birthday: %2$tm %<te, %te %<tY %te", c, c);
        System.out.println(s4);
        // Actually, this one should generate a warning
        String s5 = String.format("s5 Duke's Birthday: %<te, %te %<tY %te %12$tm ", c, c, c, c, c, c, c, c, c, c, c, c);
        System.out.println(s5);
        String s6 = String.format("s6 Duke's Birthday: %1.1f %2$te, %1$f", 1.0, c);
        System.out.println(s6);
    }
}
