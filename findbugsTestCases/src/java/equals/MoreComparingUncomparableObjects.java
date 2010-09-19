package equals;

import java.awt.Button;

import javax.swing.Icon;

public class MoreComparingUncomparableObjects {
    static enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    };

    static enum Color {
        RED, GREEN, BLUE
    };

    public static boolean foo1() {
        return Day.SUNDAY.equals(Color.RED);
    }

    public static boolean foo2() {
        Object o = Day.SUNDAY;
        return o == Color.RED;
    }

    public static boolean foo3(String s, StringBuffer b) {
        return s.equals(b);
    }

    public static boolean foo4(String s, StringBuffer b) {
        Object o = s;
        return o == b;
    }

    public static boolean falsePositive1(Object o, byte[] b) {
        return o == b;
    }

    public static boolean falsePositive2(Icon i, Button b) {
        return i == b;
    }

    public static boolean check(Icon i, Button b) {
        return i.equals(b);
    }

    public static boolean falsePositive3(String s, Comparable x) {
        return s == x;
    }
}
