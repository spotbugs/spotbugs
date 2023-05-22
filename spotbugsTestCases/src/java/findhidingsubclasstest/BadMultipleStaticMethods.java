package findhidingsubclasstest;

/**
 * This class test what happens, if there are multiple bad method overridings
 */

class SuperBadMultipleMethod {
    static void display(String s) {
        System.out.println("first method (Super) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (Super) " + s);
    }
}

class SubBadMultipleMethod extends SuperBadMultipleMethod {
    static void display(String s) {
        System.out.println("first method (sub) " + s);
    }

    static void display2(String s) {
        System.out.println("Second method (sub) " + s);
    }
}

/**
 * This test case is a non-complaint test case with multiple methods declared in both super and sub classes.
 * As the overridden methods are static, non-private and the invocation is on the instance variable.
 */
public class BadMultipleStaticMethods {
    public static void main(String[] args) {
        choose("superClass");
        choose("anything");
    }

    public static void choose(String info) {
        SuperBadMultipleMethod superClass = new SuperBadMultipleMethod();
        SuperBadMultipleMethod subClass = new SubBadMultipleMethod();
        if (info.equals("superClass")) {
            superClass.display("");
        } else {
            subClass.display("");
        }
    }
}