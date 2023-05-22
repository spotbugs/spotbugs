package findhidingsubclasstest;

class GoodGrandClass {
    static void display(String s) {
        System.out.println("Grand Information: " + s);
    }
}

class GoodParentClass extends GoodGrandClass {
}

class GoodChildClass extends GoodParentClass {
    static void display(String s) {
        System.out.println("Child information" + s);
    }
}

/**
 * This test case is a complaint test case with multiple level inheritance.
 * As the overridden methods are called using fully qualified name.
 */
public class GoodMultiLevelInheritance {
    public static void main(String[] args) {
        choose("grand");
        choose("parent");
        choose("child");
    }

    public static void choose(String info) {
        if (info.equals("grant")) {
            GoodGrandClass.display("");
        } else if (info.equals("parent")){
            GoodParentClass.display("");
        } else {
            GoodChildClass.display("");
        }
    }
}
