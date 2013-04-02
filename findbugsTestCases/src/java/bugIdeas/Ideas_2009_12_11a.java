package bugIdeas;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2009_12_11a {

    int data;

    @Override
    @ExpectWarning("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
    public boolean equals(Object that) {
        if (that instanceof Ideas_2009_12_11a)
            return data == ((Ideas_2009_12_11a) that).data;
        else if (that instanceof Integer)
            return data == ((Integer) that).intValue();
        else
            return false;
    }

    @Override
    public int hashCode() {
        return data;
    }

    @ExpectWarning("EC_ARRAY_AND_NONARRAY")
    @NoWarning(value="EC_INCOMPATIBLE_TYPES", confidence=Confidence.MEDIUM)
    public static void main(String args[]) {
        Ideas_2009_12_11a a = new Ideas_2009_12_11a();
        Ideas_2009_12_11a b = new Ideas_2009_12_11a();
        Ideas_2009_12_11a c = new Ideas_2009_12_11a();
        c.data = 1;
        System.out.println(a.equals(b));
        System.out.println(a.equals(b.data));
        System.out.println(a.equals(0));
        System.out.println(a.equals(c));
        System.out.println(a.equals(c.data));
        System.out.println(a.equals(1));
        System.out.println(a.equals(new int[] { 1 }));
    }

}
