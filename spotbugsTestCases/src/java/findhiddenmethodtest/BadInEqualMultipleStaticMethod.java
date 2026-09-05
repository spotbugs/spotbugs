package findhiddenmethodtest;

class SuperBadInEqualMultipleMethod {
    static void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }

    public static void display3() {
        System.out.println("display3 (SuperBadInEqualMultipleMethod)");
    }
}


/**
 * This test case is a non-compliant test case with multiple methods declared in both super and sub classes.
 * As the overridden methods are static, and non-private.
 * Number of methods in both classes are different.
 */
class BadInEqualMultipleStaticMethod extends SuperBadInEqualMultipleMethod {
    static void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}
