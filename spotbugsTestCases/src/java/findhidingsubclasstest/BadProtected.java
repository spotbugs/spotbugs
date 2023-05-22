package findhidingsubclasstest;

class SuperProtected {
    protected static void display(String s) {
        System.out.println("Display some information " + s);
    }
}

class SubProtected extends SuperProtected {
    protected static void display(String s) {
        System.out.println("Display some information in sub class " + s);
    }
}

/**
 * This class test what happens if the access modifier is protected for the static hiding method
 */
public class BadProtected {
    public static void main(String[] args) {
        choose("superClass");
        choose("anything");
    }

    public static void choose(String info) {
        SuperProtected superClass = new SuperProtected();
        SuperProtected subClass = new SubProtected ();
        if (info.equals("superClass")) {
            superClass.display("");
        } else {
            subClass.display("");
        }
    }
}