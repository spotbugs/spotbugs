package bugIdeas;

import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Ideas_2009_12_11 {

    int[][] data = new int[1][1];

    @Override
    @DesireWarning("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
    public boolean equals(Object that) {
        int[][] thatData;
        if (that instanceof Ideas_2009_12_11)
            thatData = ((Ideas_2009_12_11) that).data;
        else if (that instanceof int[][])
            thatData = (int[][]) that;
        else
            return false;
        return Arrays.deepEquals(data, thatData);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }

    @NoWarning(value="EC_ARRAY_AND_NONARRAY", confidence=Confidence.MEDIUM)
    @ExpectWarning(value="EC_UNRELATED_TYPES", num=2)
    public static void main(String args[]) {
        Ideas_2009_12_11 a = new Ideas_2009_12_11();
        Ideas_2009_12_11 b = new Ideas_2009_12_11();
        Ideas_2009_12_11 c = new Ideas_2009_12_11();
        c.data[0][0] = 1;
        System.out.println(a.equals(b));
        System.out.println(a.equals(b.data));
        System.out.println(a.equals(0)); // bad
        System.out.println(a.equals(c));
        System.out.println(a.equals(c.data));
        System.out.println(a.equals(1)); // bad
    }

}
