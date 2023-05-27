package findhidingsubclasstest;

class GrantAccessStatic {
    public static void displayAccountStatus() {
        System.out.println("displayAccountStatus (GrantAccessStatic)");
    }
}

/**
 * This test case is a non-compliant test case for the bug.
 * As the displayAccountStatus() is static, non-private (public) and being hidden by subclass.
 */
class GrantUserAccessStatic extends GrantAccessStatic {
    public static void displayAccountStatus() {
        System.out.println("displayAccountStatus (GrantUserAccessStatic)");
    }
}