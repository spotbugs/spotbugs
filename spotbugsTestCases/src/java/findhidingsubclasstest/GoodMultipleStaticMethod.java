package findhidingsubclasstest;

class SuperMulti {
    static void display(String s) {
        System.out.println("first method (Super)" + s);
    }

    static void display2(String s) {
        System.out.println("Second method (Super)" +s);
    }
}

class SubMulti extends SuperMulti {
    static void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}

/**
 * This class test what happen if there are multiple static methods overriding,
 * but the static methods are being called on the class name not the instance.
 */
public class GoodMultipleStaticMethod {
    public static void main(String[] args) {
        choose("superClass");
        choose("anything");
    }

    public static void choose(String info) {
        SuperMultipleNonStatic superClass = new SuperMultipleNonStatic();
        SuperMultipleNonStatic subClass = new SubMultipleNonStatic();
        if (info.equals("superClass")) {
            SuperMulti.display("");
        } else {
            SubMulti.display("");
        }
    }
}
