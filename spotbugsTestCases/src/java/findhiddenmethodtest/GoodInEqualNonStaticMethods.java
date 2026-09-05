package findhiddenmethodtest;

class SuperGoodInEqualMultipleMethod {
    void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    private void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }

    void display3() {
        System.out.println("display3 (SuperBadInEqualMultipleMethod)");
    }
}

/**
 * This test case is a compliant test case with multiple methods declared in both super and sub classes.
 * As the overridden methods are non-static.
 * Number of methods in both classes are different.
 */
class GoodInEqualNonStaticMethods extends SuperGoodInEqualMultipleMethod {
    void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}
