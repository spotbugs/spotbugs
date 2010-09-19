package bugIdeas;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2010_08_04 {
    @NoWarning("VA_FORMAT_STRING_BAD_CONVERSION")
    public static void main(String[] args) {
        java.sql.Timestamp t = new java.sql.Timestamp(System.currentTimeMillis());
        System.out.println(String.format("%tc", t));
    }

    public static void checkNonNull(Object o) {
        if (o == null)
            throw new IllegalArgumentException();
    }

    @NoWarning("NP")
    public int checkFP(int i) {
        String num = null;
        if (i > 0)
            num = Integer.toString(i);

        checkNonNull(num);
        return num.hashCode();

    }

    @ExpectWarning("NP")
    public int check(int i) {
        String num = null;
        if (i > 0)
            num = Integer.toString(i);

        return num.hashCode();

    }

}
