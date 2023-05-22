package findhidingsubclasstest;

class SuperMultipleNonStatic {
    void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    void display2(String s, int i) {
        System.out.println("Second method (Super) " + s);
    }
}

class SubMultipleNonStatic extends SuperMultipleNonStatic {
    void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}

/**
 * This test case is a complaint test case with multiple methods declared in both super and sub classes.
 * As the overridden methods are non-static.
 */
public class GoodMultipleNonStaticMethods {
    public static void main(String[] args) {
        choose("superClass");
        choose("anything");
    }

    public static void choose(String info) {
        SuperMultipleNonStatic superClass = new SuperMultipleNonStatic();
        SuperMultipleNonStatic subClass = new SubMultipleNonStatic();
        if (info.equals("superClass")) {
            superClass.display("");
        } else {
            subClass.display("");
        }
    }
}