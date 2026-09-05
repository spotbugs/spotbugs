package findhiddenmethodtest;

class SuperBadProtected {
    protected static void display(String s) {
        System.out.println("Display some information " + s);
    }
}

/**
 * This test case is a non-compliant test case for the bug.
 * As the display() is static, non-private (protected) and being hidden by subclass.
 */
class BadProtected extends SuperBadProtected {
    protected static void display(String s) {
        System.out.println("Display some information in sub class " + s);
    }
}
