package bugIdeas;

import java.util.Arrays;
import java.util.List;

public class Ideas_2009_02_24 {
    public static final int HTML_ATTREMPTY = 0x2;

    public static final int ZERO = 0;

    final static List<String> NAMES = Arrays.asList(new String[] { "John", "Bill", "Sue", "Sarah" });

    public static void main(String args[]) {
        falsePositive();
        System.out.println(makeNonnegative(Integer.MIN_VALUE));
    }

    public int maskFalsePositive() {
        int x = 0;
        return x | HTML_ATTREMPTY;
    }

    public int maskBug() {
        int x = 0;
        return x | ZERO;
    }

    public static int makeNonnegative(int x) {
        return x & 0xffffffff;
    }

    public static void falsePositive() {
        String[] a = (String[]) NAMES.toArray();
        String[] b = (String[]) Arrays.asList(new String[] { "x", "y" }).toArray();

    }

}
