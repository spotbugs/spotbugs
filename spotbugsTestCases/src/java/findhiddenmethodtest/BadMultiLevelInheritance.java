package findhiddenmethodtest;

class BadGrandClass {
    public static void display(String s) {
        System.out.println("Grand Information: " + s);
    }
}

class BadParentClass extends BadGrandClass {
}

/**
 * This test case is a non-compliant test case with multiple level inheritance.
 * As the overridden methods are static, non-private.
 * The `BadParentClass` inherit `display` method form `BadGrandClass`
 * The `BadMultiLevelInheritance` (child class) hides the `display` method.
 */
class BadMultiLevelInheritance extends BadParentClass {
    public static void display(String s) {
        BadParentClass.display("");
        System.out.println("Child information" + s);
    }
}
