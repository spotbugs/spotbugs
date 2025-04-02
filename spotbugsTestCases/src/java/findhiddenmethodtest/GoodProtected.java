package findhiddenmethodtest;

class SuperGoodProtected {
    protected void display(String s) {
        System.out.println("Display some information " + s);
    }
}


/**
 * This test case is a compliant test case for the bug.
 * As the display() is non-static, non-private (protected) and not being hidden by subclass.
 */
class GoodProtected extends SuperGoodProtected {
    protected void display(String s) {
        System.out.println("Display some information in sub class " + s);
    }
}
