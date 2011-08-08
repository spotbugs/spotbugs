package bugIdeas;

import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Ideas_2011_08_07 {

    final int x;

    Ideas_2011_08_07(int x) {
        this.x = x;
    }

    Ideas_2011_08_07 plusOne() {
        return new Ideas_2011_08_07(x + 1);
    }

    public String toString() {
        return Integer.toString(x);
    }

    @DesireWarning("")
    public static void ignoredReturnValue() {
        Ideas_2011_08_07 x = new Ideas_2011_08_07(42);
        x.plusOne();
        System.out.println(x);
    }

//    @DesireWarning("")
//    public static boolean test(@SlashedClassName String x, @DottedClassName String y) {
//        return x.equals(y);
//    }
}
