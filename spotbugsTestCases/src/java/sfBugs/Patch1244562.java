package sfBugs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patch1244562 {

    public static void main(String args[]) {
        double ok = Math.sqrt(4);
        double nan = Math.sqrt(-2);
        double ok2 = Math.atan2(1, 2);
        double ok3 = Math.log(nan);
        double nan2 = Math.log(-3);
        Pattern okpat = Pattern.compile("[a-z][^0-3]+");
        Pattern badpat = Pattern.compile("[a-z+", 0);
        Pattern badflag = Pattern.compile("[a-z]+", 0xaaaaaa);
        Matcher am = okpat.matcher("hello, world");
        am.usePattern(okpat);
        am.usePattern(null);
        try {
            String uee = new String(new byte[] { 'a', 'b' }, "unkown charset");
            String okname = new String(new byte[] { 'a', 'b' }, "iso-8859-7");
        } catch (java.io.UnsupportedEncodingException e) {
        }
        String x = "there".substring(2);
        String x2 = "there".substring(130);
        String x3 = x.concat(x2);
        String x5 = x3.substring(130);
        String x6 = x3.substring(-3);

        String a = "hello";

        // Test for a false positive report
        // The following line is correct.
        a = a.replaceAll("[aeio]", "[x");
        // Test for a false negative report.
        // The following line is wrong.
        a = a.replaceAll("[aeio", "[x]");
        System.out.println(a);

    }
}
