package findhiddenmethodtest;

class SuperGoodNonOrderedMultipleStaticMethods {
    public static void display3() {
        System.out.println("display3 (SuperBadInEqualMultipleMethod)");
    }
    void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    private void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }
}


/**
 * This test case is a compliant test case with multiple methods declared in both super and sub classes.
 * But the method definition is in different order and number of methods in both classes are different.
 * As the overridden methods are nonstatic. It is compliant.
 */
class GoodNonOrderedMultipleNonStaticMethods extends SuperGoodNonOrderedMultipleStaticMethods {
    void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}
