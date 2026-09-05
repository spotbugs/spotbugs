package findhiddenmethodtest;

class SuperGoodMultipleStaticMethod {
    private static void display(String s) {
        System.out.println("first method (Super)" + s);
    }

    private static void display2(String s) {
        System.out.println("Second method (Super)" +s);
    }
}

/**
 * This class test is a compliant test case for multiple static method declarations,
 * but the static methods are declared as private.
 * So there is no method inherited actually
 */
class GoodMultipleStaticMethod extends SuperGoodMultipleStaticMethod {
    private static void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    private static void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}
