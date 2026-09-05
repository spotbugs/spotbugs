package findhiddenmethodtest;

class SuperBadNonOrderedMultipleStaticMethods {
    public static void display3() {
        System.out.println("display3 (SuperBadInEqualMultipleMethod)");
    }
    static void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }
}


/**
 * This test case is a non-compliant test case with multiple methods declared in both super and sub classes.
 * But the method definition is in different order and number of methods in both classes are different.
 * As the overridden methods are static, and non-private. It is non-compliant.
 */
class BadNonOrderedMultipleStaticMethods extends SuperBadNonOrderedMultipleStaticMethods {
    static void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}
