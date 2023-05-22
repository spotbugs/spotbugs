package findhidingsubclasstest;

class BadGrandClass {
    static void display(String s) {
        System.out.println("Grand Information: " + s);
    }
}

class BadParentClass extends BadGrandClass {
}

class BadChildClass extends BadParentClass {
    static void display(String s) {
        System.out.println("Child information" + s);
    }
}

/**
 * This test case is a non-complaint test case with multiple level inheritance.
 * As the overridden methods are static, non-private and invocation is on instance variables..
 */
public class BadMultiLevelInheritance {
    public static void main(String[] args) {
        choose("grant");
        choose("parent");
        choose("child");
    }

    public static void choose(String info) {
        BadGrandClass grandClass = new BadGrandClass();
        BadGrandClass parentClass = new BadParentClass();
        BadGrandClass childClass = new BadChildClass();
        if (info.equals("grant")) {
            grandClass.display("");
        } else if (info.equals("parent")){
            parentClass.display("");
        } else {
            childClass.display("");
        }
    }
}
