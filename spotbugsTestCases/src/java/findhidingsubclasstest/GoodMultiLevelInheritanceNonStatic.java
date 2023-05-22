package findhidingsubclasstest;

class GoodGrandNonStatic {
    public void display(String s, int i) {
        System.out.println("Grand Information: ");
    }
}

class GoodParentNonStatic extends GoodGrandNonStatic {
}

class GoodChildNonStatic extends GoodParentNonStatic {
    public void display(String s, int i) {
        System.out.println("Child information");
    }
}

/**
 * This test case is a complaint test case with multiple level inheritance.
 * As the overridden methods are non-static.
 */
public class GoodMultiLevelInheritanceNonStatic {
    public static void main(String[] args) {
        choose("grand");
        choose("parent");
        choose("child");
    }

    public static void choose(String info) {
        GoodGrandNonStatic grandClass = new GoodGrandNonStatic();
        GoodGrandNonStatic parentClass = new GoodParentNonStatic();
        GoodGrandNonStatic childClass = new GoodChildNonStatic();
        if (info.equals("grant")) {
            grandClass.display("", 0);
        } else if (info.equals("parent")){
            parentClass.display("", 0);
        } else {
            childClass.display("", 0);
        }
    }
}
