package findhidingsubclasstest;

class SuperBadMultipleMethods {
    static void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }
}

/**
 * This test case is a non-complaint test case with multiple methods declared in both super and sub classes.
 * As the overridden methods are static, and non-private.
 */
class SubBadMultipleMethods extends SuperBadMultipleMethods {
    static void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}

public class BadMultipleStaticMethods {
}