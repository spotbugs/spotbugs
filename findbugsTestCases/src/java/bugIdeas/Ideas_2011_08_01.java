package bugIdeas;

import java.sql.Time;

import com.google.common.primitives.Booleans;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedBytes;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Ideas_2011_08_01 {

    @DesireNoWarning("VA_FORMAT_STRING_BAD_CONVERSION")
    public static void main(String args[]) {
        Time t = new Time(System.currentTimeMillis());
        System.out.printf("%tr%n", t);
    }

    @ExpectWarning(value="RV_CHECK_COMPARETO_FOR_SPECIFIC_RETURN_VALUE", num = 9)
    public static int testGuavaPrimitiveCompareCalls() {
        int count = 0;
        if (Booleans.compare(false, true) == -1)
            count++;
        if (Chars.compare('a', 'b') == -1)
            count++;
        if (Doubles.compare(1, 2) == -1)
            count++;
        if (Floats.compare(1, 2) == -1)
            count++;
        if (Ints.compare(1, 2) == -1)
            count++;
        if (Longs.compare(1, 2) == -1)
            count++;
        if (Shorts.compare((short) 1, (short) 2) == -1)
            count++;
        if (SignedBytes.compare((byte) 1, (byte) 2) == -1)
            count++;
        if (UnsignedBytes.compare((byte) 1, (byte) 2) == -1)
            count++;
        return count;

    }

}
