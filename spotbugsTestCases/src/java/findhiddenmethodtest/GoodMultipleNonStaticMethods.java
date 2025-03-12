package findhiddenmethodtest;

class SuperMultipleNonStatic {
    void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }
}

/**
 * This test case is a compliant test case with multiple methods declared in both super and sub classes.
 * As the overridden methods are non-static and non-private.
 * So, this is overriding instead of method hiding.
 */
class GoodMultipleNonStaticMethods extends SuperMultipleNonStatic {
    void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}
