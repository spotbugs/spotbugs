package findhidingsubclasstest;

class BadSuperClass
{
    public static void methodHiding() {
        System.out.println("methodHiding (BadSuperClass)");
    }
    public void methodOverriding() {
        System.out.println("methodOverriding (BadSuperClass)");
    }
}

class BadSubClass extends BadSuperClass
{
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
 * Run this class and observe the output on the console to better understand.
 */
public class BadHidingVsOverriding {
    public static void main(String[] args) {
        BadSuperClass d1 = new BadSuperClass();
        //d2 is reference variable of class BadSuperClass that points to object of class Sample
        BadSuperClass d2 = new BadSubClass();
        //method calling with reference (method hiding)
        d1.methodHiding();
        d2.methodHiding();
        //method calling with object (method overriding)
        d1.methodOverriding();
        d2.methodOverriding();
    }
}