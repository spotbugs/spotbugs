package findhiddenmethodtest;

class BadSuperClass {
    public static void methodHiding() {
        System.out.println("methodHiding (BadSuperClass)");
    }
    public void methodOverriding() {
        System.out.println("methodOverriding (BadSuperClass)");
    }
}

/**
 * This class is the non-compliant test case for the bug.
 * This test case also clarifies the confusion of method overriding and method hiding.
 * As the `methodHiding` is being hidden while `methodOverriding` is being overridden.
 */
class BadHidingVsOverriding extends BadSuperClass {
    public static void methodHiding() {
        System.out.println("methodHiding (BadSuperClass)");
    }
    public void methodOverriding() {
        System.out.println("methodOverriding (BadSuperClass)");
    }
}
