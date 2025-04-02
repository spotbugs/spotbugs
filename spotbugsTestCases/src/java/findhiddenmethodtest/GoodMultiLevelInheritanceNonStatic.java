package findhiddenmethodtest;

class GoodGrandNonStatic {
    public void display(String s, int i) {
        System.out.println("Grand Information: ");
    }
}

class GoodParentNonStatic extends GoodGrandNonStatic {
}

/**
 * This test case is a compliant test case with multiple level inheritance.
 * As the overridden methods are non-static.
 */
class GoodMultiLevelInheritanceNonStatic extends GoodParentNonStatic {
    public void display(String s, int i) {
        System.out.println("Child information");
    }
}
